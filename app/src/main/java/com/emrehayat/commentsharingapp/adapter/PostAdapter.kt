package com.emrehayat.commentsharingapp.adapter

import android.icu.text.SimpleDateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.emrehayat.commentsharingapp.databinding.RecyclerRowBinding
import com.emrehayat.commentsharingapp.model.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import java.util.Locale

class PostAdapter(private val postList: ArrayList<Post>) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    class PostViewHolder(val binding: RecyclerRowBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = RecyclerRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(binding)
    }

    override fun getItemCount(): Int = postList.size

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = postList[position]
        
        holder.binding.apply {
            kullaniciAdiRecyclerView.text = post.userName
            
            // Handle comment visibility
            if (post.comment.isNotEmpty()) {
                yorumRecyclerView.visibility = View.VISIBLE
                yorumRecyclerView.text = post.comment
            } else {
                yorumRecyclerView.visibility = View.GONE
            }

            // Format and set date
            val date = post.date.toDate()
            val formattedDate = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(date)
            tarihRecyclerView.text = formattedDate

            // Load and display image
            Picasso.get().load(post.downloadUrl).into(imageView2)

            // Handle delete button visibility
            if (post.userId == auth.currentUser?.uid) {
                deleteButton.visibility = View.VISIBLE
                deleteButton.setOnClickListener {
                    deletePost(post, position, holder.itemView)
                }
            } else {
                deleteButton.visibility = View.GONE
            }
        }
    }

    private fun deletePost(post: Post, position: Int, itemView: View) {
        db.collection("Posts")
            .whereEqualTo("downloadUrl", post.downloadUrl)
            .whereEqualTo("userId", auth.currentUser?.uid)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    document.reference.delete()
                        .addOnSuccessListener {
                            postList.removeAt(position)
                            notifyItemRemoved(position)
                            notifyItemRangeChanged(position, postList.size)
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(itemView.context, "Gönderi silinemedi: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                }
            }
    }
}