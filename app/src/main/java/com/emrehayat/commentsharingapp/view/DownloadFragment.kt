package com.emrehayat.commentsharingapp.view

import android.Manifest
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.Navigation
import com.emrehayat.commentsharingapp.databinding.FragmentDownloadBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import java.util.UUID

class DownloadFragment : Fragment() {

    private var _binding: FragmentDownloadBinding? = null
    private val binding get() = _binding!!
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    var selectedImage : Uri? = null
    var selectedBitmap : Bitmap? = null
    private lateinit var userName: String

    private lateinit var auth : FirebaseAuth
    private lateinit var storage: FirebaseStorage
    private lateinit var db : FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
        storage = Firebase.storage
        db = Firebase.firestore

        registerLaunchers()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        userName = auth.currentUser?.displayName ?: "Unknown User"
        _binding = FragmentDownloadBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.yukleButton.setOnClickListener { yuklemeyiYap(it) }
        binding.imageView.setOnClickListener { gorseliSec(it) }
    }

    fun yuklemeyiYap(view: View) {
        val userName = auth.currentUser?.displayName ?: "Unknown User"
        val userId = auth.currentUser?.uid
        
        if (selectedImage == null) {
            Toast.makeText(requireContext(), "Lütfen bir görsel seçin.", Toast.LENGTH_LONG).show()
            return
        }

        if (userId == null) {
            Toast.makeText(requireContext(), "Kullanıcı girişi yapılmamış.", Toast.LENGTH_LONG).show()
            return
        }

        val uuid = UUID.randomUUID()
        val imageName = "$uuid.jpg"
        val imageReference = storage.reference.child("images").child(imageName)

        imageReference.putFile(selectedImage!!).addOnSuccessListener {
            imageReference.downloadUrl.addOnSuccessListener { uri ->
                val downloadUrl = uri.toString()
                val comment = binding.yorumText.text.toString()
                
                val postMap = hashMapOf(
                    "downloadUrl" to downloadUrl,
                    "comment" to comment,
                    "date" to Timestamp.now(),
                    "userName" to userName,
                    "userId" to userId
                )

                db.collection("Posts")
                    .add(postMap)
                    .addOnSuccessListener {
                        val action = DownloadFragmentDirections.actionDownloadFragmentToFeedFragment()
                        Navigation.findNavController(view).navigate(action)
                    }
                    .addOnFailureListener { exception ->
                        Toast.makeText(requireContext(), exception.localizedMessage, Toast.LENGTH_LONG).show()
                    }
            }
        }.addOnFailureListener { exception ->
            Toast.makeText(requireContext(), exception.localizedMessage, Toast.LENGTH_LONG).show()
        }
    }

    fun gorseliSec(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            //read media images
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                //izin yok
                if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.READ_MEDIA_IMAGES)) {
                    Snackbar.make(view, "Galeriye gitmek için izin vermelisiniz!", Snackbar.LENGTH_INDEFINITE).setAction("İzin Ver", View.OnClickListener {
                        permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                    }).show()
                } else {
                    permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                }
            } else {
                val intentToGallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)
            }
        } else {
            //read external storage
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                //izin yok
                if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    Snackbar.make(view, "Galeriye gitmek için izin vermelisiniz!", Snackbar.LENGTH_INDEFINITE).setAction("İzin Ver", View.OnClickListener {
                        permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }).show()
                } else {
                    permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            } else {
                val intentToGallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)
            }
        }
    }

    private fun registerLaunchers() {
        activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val intentFromResult = result.data
                if (intentFromResult != null) {
                    selectedImage = intentFromResult.data
                    try {
                        if (Build.VERSION.SDK_INT >= 28) {
                            val source = ImageDecoder.createSource(requireActivity().contentResolver, selectedImage!!)
                            selectedBitmap = ImageDecoder.decodeBitmap(source)
                            binding.imageView.setImageBitmap(selectedBitmap)
                        } else {
                            selectedBitmap = MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, selectedImage)
                            binding.imageView.setImageBitmap(selectedBitmap)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { result->
            if (result) {
                //izin verildi
                val intentToGallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)
            } else {
                //kullanıcı izni reddetti
                Toast.makeText(requireContext(), "İzni reddettiniz. Lütfen, izin veriniz!", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}

/*class DownloadFragment : Fragment() {

    private var _binding: FragmentDownloadBinding? = null
    private val binding get() = _binding!!
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    var selectedImage : Uri? = null
    var selectedBitmap : Bitmap? = null

    private lateinit var auth : FirebaseAuth
    private lateinit var storage: FirebaseStorage
    private lateinit var db : FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
        storage = Firebase.storage
        db = Firebase.firestore
        registerLaunchers()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentDownloadBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.yukleButton.setOnClickListener { yuklemeyiYap(it) }
        binding.imageView.setOnClickListener { gorseliSec(it) }
    }

    fun yuklemeyiYap(view: View) {
        // Kullanıcı adını doğrudan auth.currentUser üzerinden alıyoruz.
        val userName = auth.currentUser?.displayName ?: "Unknown User"

        val uuid = UUID.randomUUID()
        val imageName = "$uuid.jpg"
        val imageReference = storage.reference.child("images").child(imageName)

        selectedImage?.let {
            imageReference.putFile(it).addOnSuccessListener {
                imageReference.downloadUrl.addOnSuccessListener { uri ->
                    val downloadUrl = uri.toString()
                    val postMap = hashMapOf(
                        "downloadUrl" to downloadUrl,
                        "comment" to binding.yorumText.text.toString(),
                        "date" to Timestamp.now(),
                        "userName" to userName
                    )
                    db.collection("Posts").add(postMap).addOnSuccessListener {
                        val action = DownloadFragmentDirections.actionDownloadFragmentToFeedFragment()
                        Navigation.findNavController(view).navigate(action)
                    }.addOnFailureListener { exception ->
                        Toast.makeText(requireContext(), exception.localizedMessage, Toast.LENGTH_LONG).show()
                    }
                }
            }.addOnFailureListener { exception ->
                Toast.makeText(requireContext(), exception.localizedMessage, Toast.LENGTH_LONG).show()
            }
        } ?: Toast.makeText(requireContext(), "Lütfen bir görsel seçin.", Toast.LENGTH_LONG).show()
    }

    fun gorseliSec(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.READ_MEDIA_IMAGES)) {
                    Snackbar.make(view, "Galeriye gitmek için izin vermelisiniz!", Snackbar.LENGTH_INDEFINITE).setAction("İzin Ver") {
                        permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                    }.show()
                } else {
                    permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                }
            } else {
                val intentToGallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)
            }
        } else {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    Snackbar.make(view, "Galeriye gitmek için izin vermelisiniz!", Snackbar.LENGTH_INDEFINITE).setAction("İzin Ver") {
                        permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }.show()
                } else {
                    permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            } else {
                val intentToGallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)
            }
        }
    }

    private fun registerLaunchers() {
        activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val intentFromResult = result.data
                if (intentFromResult != null) {
                    selectedImage = intentFromResult.data
                    try {
                        if (Build.VERSION.SDK_INT >= 28) {
                            val source = ImageDecoder.createSource(requireActivity().contentResolver, selectedImage!!)
                            selectedBitmap = ImageDecoder.decodeBitmap(source)
                            binding.imageView.setImageBitmap(selectedBitmap)
                        } else {
                            selectedBitmap = MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, selectedImage)
                            binding.imageView.setImageBitmap(selectedBitmap)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
            if (result) {
                val intentToGallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)
            } else {
                Toast.makeText(requireContext(), "İzni reddettiniz. Lütfen, izin veriniz!", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}*/