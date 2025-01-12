package com.emrehayat.commentsharingapp.view

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.emrehayat.commentsharingapp.R
import com.emrehayat.commentsharingapp.adapter.PostAdapter
import com.emrehayat.commentsharingapp.databinding.FragmentFeedBinding
import com.emrehayat.commentsharingapp.model.Post
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class FeedFragment : Fragment() , PopupMenu.OnMenuItemClickListener {

    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!
    private lateinit var popup : PopupMenu
    private lateinit var auth : FirebaseAuth
    private lateinit var db : FirebaseFirestore
    val postList : ArrayList<Post> = arrayListOf()
    private var adapter : PostAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
        db = Firebase.firestore
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentFeedBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.floatingActionButton.setOnClickListener { floatingActionButtonaTiklandi(it) }

        popup = PopupMenu(requireContext(), binding.floatingActionButton)
        val inflater = popup.menuInflater
        inflater.inflate(R.menu.my_popup_menu, popup.menu)
        popup.setOnMenuItemClickListener(this)

        verileriAlFirestore()

        adapter = PostAdapter(postList)
        binding.commentRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.commentRecyclerView.adapter = adapter
    }

    private fun verileriAlFirestore() {
        db.collection("Posts")
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    Toast.makeText(requireContext(), error.localizedMessage, Toast.LENGTH_LONG).show()
                } else {
                    if (value != null) {
                        if (!value.isEmpty) {
                            postList.clear()
                            val documents = value.documents
                            for (document in documents) {
                                val userName = document.get("userName") as? String ?: "Unknown User"
                                val comment = document.get("comment") as? String ?: ""
                                val downloadUrl = document.get("downloadUrl") as String
                                val date = document.get("date") as Timestamp
                                val userId = document.get("userId") as? String
                                val isDeleted = document.get("isDeleted") as? Boolean ?: false

                                if (!isDeleted) {
                                    val post = Post(userName, comment, downloadUrl, date, userId, isDeleted)
                                    postList.add(post)
                                }
                            }
                            adapter?.notifyDataSetChanged()
                        }
                    }
                }
            }
    }

    /*private fun verileriAlFirestore() {
        db.collection("Posts").orderBy("date", Query.Direction.DESCENDING).addSnapshotListener { value, error ->
            if (error != null) {
                Toast.makeText(requireContext(), error.localizedMessage, Toast.LENGTH_LONG).show()
            } else {
                if (value != null && !value.isEmpty) {
                    postList.clear()
                    val documents = value.documents
                    for (document in documents) {
                        // Değerleri güvenli bir şekilde alma
                        val userName = document.getString("userName") ?: ""
                        val comment = document.getString("comment") ?: ""
                        val downloadUrl = document.getString("downloadUrl") ?: ""
                        val date = document.getTimestamp("date") ?: Timestamp.now()

                        val post = Post(userName, comment, downloadUrl, date)
                        postList.add(post)
                    }
                    adapter?.notifyDataSetChanged()
                }
            }
        }
    }*/


    fun floatingActionButtonaTiklandi(view: View) {
        popup.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.yeniPaylasimItem -> {
                val action = FeedFragmentDirections.actionFeedFragmentToDownloadFragment()
                Navigation.findNavController(requireView()).navigate(action)
            }
            R.id.profilItem -> {
                val action = FeedFragmentDirections.actionFeedFragmentToProfileFragment()
                Navigation.findNavController(requireView()).navigate(action)
            }
            R.id.cikisYapItem -> {
                auth.signOut()
                val action = FeedFragmentDirections.actionFeedFragmentToUserFragment()
                Navigation.findNavController(requireView()).navigate(action)
            }
        }
        return true
    }

}