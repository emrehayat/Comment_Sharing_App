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
import com.google.firebase.firestore.FieldValue
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
        val currentUserId = auth.currentUser?.uid
        
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
            if (post.userId == currentUserId) {
                deleteButton.visibility = View.VISIBLE
                deleteButton.setOnClickListener {
                    deletePost(post, position, holder.itemView)
                }
            } else {
                deleteButton.visibility = View.GONE
            }

            // Handle like button
            likeCount.text = post.likes.size.toString()
            likeButton.setImageResource(
                if (currentUserId != null && post.likes.contains(currentUserId))
                    android.R.drawable.btn_star_big_on
                else
                    android.R.drawable.btn_star_big_off
            )
            likeButton.setOnClickListener {
                if (currentUserId != null) {
                    toggleLike(post, currentUserId, holder.itemView)
                }
            }

            // Handle save button
            saveButton.setImageResource(
                if (currentUserId != null && post.saves.contains(currentUserId))
                    android.R.drawable.ic_menu_save
                else
                    android.R.drawable.ic_menu_set_as
            )
            saveButton.setOnClickListener {
                if (currentUserId != null) {
                    toggleSave(post, currentUserId, holder.itemView)
                }
            }

            // Handle comment button and show comments button
            commentButton.setOnClickListener {
                // TODO: Implement comment dialog
                Toast.makeText(holder.itemView.context, "Yorum yapma özelliği yakında eklenecek", Toast.LENGTH_SHORT).show()
            }

            showCommentsButton.text = if (post.commentCount > 0) {
                "Yorumları Göster (${post.commentCount})"
            } else {
                "Yorum Yap"
            }
            showCommentsButton.setOnClickListener {
                // TODO: Show comments dialog
                Toast.makeText(holder.itemView.context, "Yorumlar yakında eklenecek", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun toggleLike(post: Post, userId: String, itemView: View) {
        db.collection("Posts")
            .whereEqualTo("downloadUrl", post.downloadUrl)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    if (post.likes.contains(userId)) {
                        // Unlike
                        document.reference.update("likes", FieldValue.arrayRemove(userId))
                            .addOnSuccessListener {
                                post.likes.remove(userId)
                                notifyDataSetChanged()
                            }
                    } else {
                        // Like
                        document.reference.update("likes", FieldValue.arrayUnion(userId))
                            .addOnSuccessListener {
                                post.likes.add(userId)
                                notifyDataSetChanged()
                            }
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(itemView.context, "Hata: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }

    private fun toggleSave(post: Post, userId: String, itemView: View) {
        db.collection("Posts")
            .whereEqualTo("downloadUrl", post.downloadUrl)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    if (post.saves.contains(userId)) {
                        // Unsave
                        document.reference.update("saves", FieldValue.arrayRemove(userId))
                            .addOnSuccessListener {
                                post.saves.remove(userId)
                                notifyDataSetChanged()
                            }
                    } else {
                        // Save
                        document.reference.update("saves", FieldValue.arrayUnion(userId))
                            .addOnSuccessListener {
                                post.saves.add(userId)
                                notifyDataSetChanged()
                            }
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(itemView.context, "Hata: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }

    private fun deletePost(post: Post, position: Int, itemView: View) {
        db.collection("Posts")
            .whereEqualTo("downloadUrl", post.downloadUrl)
            .whereEqualTo("userId", auth.currentUser?.uid)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    document.reference.update("isDeleted", true)
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