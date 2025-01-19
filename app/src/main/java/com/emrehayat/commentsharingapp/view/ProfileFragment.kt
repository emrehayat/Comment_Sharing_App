package com.emrehayat.commentsharingapp.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.Navigation
import com.emrehayat.commentsharingapp.R
import com.emrehayat.commentsharingapp.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore
    private val storage = Firebase.storage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.editTextUsername.setText(auth.currentUser?.displayName)
        
        binding.buttonUpdateUsername.setOnClickListener {
            val newUsername = binding.editTextUsername.text.toString()
            if (newUsername.isNotEmpty()) {
                updateUsername(newUsername)
            } else {
                Toast.makeText(requireContext(), "Lütfen kullanıcı adı giriniz", Toast.LENGTH_SHORT).show()
            }
        }

        binding.buttonDeleteAccount.setOnClickListener {
            showDeleteAccountConfirmationDialog()
        }

        binding.buttonDeletePosts.setOnClickListener {
            showDeletePostsConfirmationDialog()
        }

        binding.buttonDeletedPosts.setOnClickListener {
            showDeletedPosts()
        }

        binding.buttonGoToFeed.setOnClickListener {
            Navigation.findNavController(it).navigate(R.id.action_profileFragment_to_feedFragment)
        }

        binding.buttonLikedPosts.setOnClickListener {
            Navigation.findNavController(it).navigate(R.id.action_profileFragment_to_likedPostsFragment)
        }

        binding.buttonSavedPosts.setOnClickListener {
            Navigation.findNavController(it).navigate(R.id.action_profileFragment_to_savedPostsFragment)
        }
    }

    private fun updateUsername(newUsername: String) {
        val user = auth.currentUser
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(newUsername)
            .build()

        user?.updateProfile(profileUpdates)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(requireContext(), "Kullanıcı adı güncellendi", Toast.LENGTH_SHORT).show()
                    updateUsernameInPosts(newUsername)
                } else {
                    Toast.makeText(requireContext(), "Hata: ${task.exception?.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun updateUsernameInPosts(newUsername: String) {
        val userId = auth.currentUser?.uid ?: return
        
        db.collection("Posts")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    document.reference.update("userName", newUsername)
                }
            }
    }

    private fun deleteUserPosts() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(requireContext(), "Kullanıcı girişi yapılmamış.", Toast.LENGTH_LONG).show()
            return
        }

        db.collection("Posts")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(requireContext(), "Silinecek gönderi bulunamadı", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                var deletedCount = 0
                val totalPosts = documents.size()

                for (document in documents) {
                    document.reference.update("isDeleted", true)
                        .addOnSuccessListener {
                            deletedCount++
                            if (deletedCount == totalPosts) {
                                Toast.makeText(requireContext(), "Tüm gönderiler silindi", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(requireContext(), "Hata: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Hata: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }

    private fun deleteUserAccount() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(requireContext(), "Kullanıcı girişi yapılmamış.", Toast.LENGTH_LONG).show()
            return
        }

        // Önce kullanıcının gönderilerini silelim
        db.collection("Posts")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->
                // Gönderi olmasa bile hesabı silebiliriz
                if (documents.isEmpty) {
                    deleteAccountFromAuth()
                    return@addOnSuccessListener
                }

                var deletedCount = 0
                val totalPosts = documents.size()

                documents.forEach { document ->
                    val downloadUrl = document.getString("downloadUrl")
                    if (downloadUrl != null) {
                        try {
                            val imageRef = storage.getReferenceFromUrl(downloadUrl)
                            imageRef.delete().addOnSuccessListener {
                                document.reference.delete()
                                    .addOnSuccessListener {
                                        deletedCount++
                                        if (deletedCount == totalPosts) {
                                            deleteAccountFromAuth()
                                        }
                                    }
                            }.addOnFailureListener {
                                // Resim silinmese bile postu ve hesabı silelim
                                document.reference.delete()
                                    .addOnSuccessListener {
                                        deletedCount++
                                        if (deletedCount == totalPosts) {
                                            deleteAccountFromAuth()
                                        }
                                    }
                            }
                        } catch (e: Exception) {
                            // URL geçersiz olsa bile postu ve hesabı silelim
                            document.reference.delete()
                                .addOnSuccessListener {
                                    deletedCount++
                                    if (deletedCount == totalPosts) {
                                        deleteAccountFromAuth()
                                    }
                                }
                        }
                    } else {
                        // Resim URL'i yoksa direkt postu sil ve devam et
                        document.reference.delete()
                            .addOnSuccessListener {
                                deletedCount++
                                if (deletedCount == totalPosts) {
                                    deleteAccountFromAuth()
                                }
                            }
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Hata: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }

    private fun deleteAccountFromAuth() {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(requireContext(), "Kullanıcı girişi yapılmamış.", Toast.LENGTH_LONG).show()
            return
        }

        // Kullanıcının yeniden kimlik doğrulaması yapması gerekebilir
        user.delete()
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Hesabınız silindi", Toast.LENGTH_SHORT).show()
                Navigation.findNavController(requireView()).navigate(R.id.action_profileFragment_to_userFragment)
            }
            .addOnFailureListener { e ->
                if (e.message?.contains("requires recent authentication") == true) {
                    Toast.makeText(requireContext(), "Lütfen çıkış yapıp tekrar giriş yapın ve sonra hesabı silmeyi deneyin", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(requireContext(), "Hesap silinemedi: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun showDeletePostsConfirmationDialog() {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Gönderileri Sil")
            .setMessage("Tüm gönderileriniz silinecek. Bu işlem geri alınamaz. Emin misiniz?")
            .setPositiveButton("Evet") { _, _ ->
                deleteUserPosts()
            }
            .setNegativeButton("Hayır", null)
            .show()
    }

    private fun showDeleteAccountConfirmationDialog() {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Hesabı Sil")
            .setMessage("Hesabınız ve tüm gönderileriniz kalıcı olarak silinecek. Bu işlem geri alınamaz. Emin misiniz?")
            .setPositiveButton("Evet") { _, _ ->
                deleteUserAccount()
            }
            .setNegativeButton("Hayır", null)
            .show()
    }

    private fun showDeletedPosts() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(requireContext(), "Kullanıcı girişi yapılmamış.", Toast.LENGTH_LONG).show()
            return
        }

        db.collection("Posts")
            .whereEqualTo("userId", userId)
            .whereEqualTo("isDeleted", true)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(requireContext(), "Silinen gönderi bulunamadı", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val deletedPosts = documents.mapNotNull { document ->
                    val userName = document.get("userName") as? String ?: return@mapNotNull null
                    val comment = document.get("comment") as? String ?: ""
                    
                    Triple(document.id, userName, comment)
                }

                if (deletedPosts.isEmpty()) {
                    Toast.makeText(requireContext(), "Silinen gönderi bulunamadı", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                showDeletedPostsDialog(deletedPosts)
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Hata: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }

    private fun showDeletedPostsDialog(deletedPosts: List<Triple<String, String, String>>) {
        val postItems = deletedPosts.map { (_, userName, comment) ->
            if (comment.isNotEmpty()) {
                "📝 $userName: $comment"
            } else {
                "🖼️ $userName (Sadece Görsel)"
            }
        }.toTypedArray()

        val message = StringBuilder()
        message.append("Silinen Gönderiler:\n\n")
        postItems.forEachIndexed { index, post ->
            message.append("${index + 1}. $post\n\n")
        }
        message.append("\nGeri yüklemek istediğiniz gönderinin numarasını seçin:")

        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setTitle("Silinen Gönderiler")
            .setMessage(message.toString())
            .setPositiveButton("Geri Yükle") { _, _ ->
                showPostSelectionDialog(deletedPosts)
            }
            .setNegativeButton("Kapat", null)
            .show()
    }

    private fun showPostSelectionDialog(deletedPosts: List<Triple<String, String, String>>) {
        val postItems = deletedPosts.map { (_, userName, comment) ->
            if (comment.isNotEmpty()) {
                "📝 $userName: $comment"
            } else {
                "🖼️ $userName (Sadece Görsel)"
            }
        }.toTypedArray()

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Gönderi Seç")
            .setItems(postItems) { _, position ->
                showRestoreConfirmationDialog(deletedPosts[position].first)
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun showRestoreConfirmationDialog(documentId: String) {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Gönderiyi Geri Yükle")
            .setMessage("Bu gönderi ana sayfanızda tekrar görünür olacak. Geri yüklemek istediğinize emin misiniz?")
            .setPositiveButton("Evet, Geri Yükle") { _, _ ->
                restorePost(documentId)
            }
            .setNegativeButton("Vazgeç", null)
            .show()
    }

    private fun restorePost(documentId: String) {
        db.collection("Posts")
            .document(documentId)
            .update("isDeleted", false)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Gönderi geri yüklendi", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Hata: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 