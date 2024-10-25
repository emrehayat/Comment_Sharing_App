package com.emrehayat.commentsharingapp.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.Navigation
import com.emrehayat.commentsharingapp.databinding.FragmentSignUpBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class SignUpFragment : Fragment() {

    private var _binding: FragmentSignUpBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSignUpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.kayitButton2.setOnClickListener { kayitOlundu(it) }
    }

    private fun kayitOlundu(view: View) {
        val email = binding.mailText2.text.toString()
        val password = binding.sifreText2.text.toString()
        val userName = binding.kullaniciAdiText.text.toString()

        if (email.isNotEmpty() && password.isNotEmpty() && userName.isNotEmpty()) {
            auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(userName).build()

                    auth.currentUser?.updateProfile(profileUpdates)?.addOnCompleteListener { updateTask ->
                        if (updateTask.isSuccessful) {
                            Toast.makeText(requireContext(), "Kayıt başarılı!", Toast.LENGTH_SHORT).show()
                            val action = SignUpFragmentDirections.actionSignUpFragmentToFeedFragment()
                            Navigation.findNavController(view).navigate(action)
                        } else {
                            Toast.makeText(requireContext(), "Kullanıcı adı güncellenemedi: ${updateTask.exception?.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    Toast.makeText(requireContext(), task.exception?.localizedMessage, Toast.LENGTH_LONG).show()
                }
            }
        } else {
            Toast.makeText(requireContext(), "Lütfen ilgili yerleri doldurunuz!", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}