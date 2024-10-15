package com.emrehayat.commentsharingapp.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.Navigation
import com.emrehayat.commentsharingapp.databinding.FragmentUserBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class UserFragment : Fragment() {

    private var _binding: FragmentUserBinding? = null
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
        _binding = FragmentUserBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.girisButton.setOnClickListener { girisYap(it) }
        binding.kayitButton.setOnClickListener { kayitOlaGit(it) }

        val guncelKullanici = auth.currentUser
        if (guncelKullanici != null) {
            // Kullanıcı zaten kayıtlıysa ve çıkış yapmamışsa direkt ana ekrana git.
            val action = UserFragmentDirections.actionUserFragmentToFeedFragment()
            Navigation.findNavController(view).navigate(action)
        }

    }

    fun girisYap(view: View) {
        val email = binding.mailText.text.toString()
        val password = binding.sifreText.text.toString()

        if (email.isNotEmpty() && password.isNotEmpty()) {
            auth.signInWithEmailAndPassword(email, password).addOnSuccessListener {
                val action = UserFragmentDirections.actionUserFragmentToFeedFragment()
                Navigation.findNavController(view).navigate(action)
            }.addOnFailureListener { exception ->
                Toast.makeText(requireContext(), exception.localizedMessage, Toast.LENGTH_LONG).show()
            }
        }
    }

    fun kayitOlaGit(view: View) {
        val action = UserFragmentDirections.actionUserFragmentToSignUpFragment()
        Navigation.findNavController(view).navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}