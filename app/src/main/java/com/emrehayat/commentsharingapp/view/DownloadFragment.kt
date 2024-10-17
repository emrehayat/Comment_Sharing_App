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
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.yukleButton.setOnClickListener { yuklemeyiYap(it) }
        binding.imageView.setOnClickListener { gorseliSec(it) }
    }

    fun yuklemeyiYap(view: View) {
        val uuid = UUID.randomUUID()
        val imageName = "${uuid}.jpg"

        val reference = storage.reference
        val imageReference = reference.child("images").child(imageName)
        if (selectedImage != null) {
            imageReference.putFile(selectedImage!!).addOnSuccessListener { uploadTask ->
                imageReference.downloadUrl.addOnSuccessListener { uri ->
                    if (auth.currentUser != null) {
                        val downloadUrl = uri.toString()
                        val postMap = hashMapOf<String, Any>()
                        postMap.put("downloadUrl", downloadUrl)
                        postMap.put("comment", binding.yorumText.text.toString())
                        postMap.put("date", Timestamp.now())

                        db.collection("Posts").add(postMap).addOnSuccessListener { documentReference ->
                            val action = DownloadFragmentDirections.actionDownloadFragmentToFeedFragment()
                            Navigation.findNavController(view).navigate(action)
                        }.addOnFailureListener { exception ->
                            Toast.makeText(requireContext(), exception.localizedMessage, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }.addOnFailureListener { exception ->
                Toast.makeText(requireContext(), exception.localizedMessage, Toast.LENGTH_LONG).show()
            }
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