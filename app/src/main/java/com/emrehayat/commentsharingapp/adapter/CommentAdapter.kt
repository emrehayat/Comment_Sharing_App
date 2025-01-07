package com.emrehayat.commentsharingapp.adapter

import android.icu.text.SimpleDateFormat
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.emrehayat.commentsharingapp.databinding.ItemCommentBinding
import com.emrehayat.commentsharingapp.model.Comment
import java.util.Locale

class CommentAdapter(private val comments: ArrayList<Comment>) : 
    RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    class CommentViewHolder(val binding: ItemCommentBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val binding = ItemCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CommentViewHolder(binding)
    }

    override fun getItemCount(): Int = comments.size

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = comments[position]
        holder.binding.apply {
            textUsername.text = comment.username
            textComment.text = comment.commentText
            
            val date = comment.timestamp.toDate()
            val formattedDate = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(date)
            textTimestamp.text = formattedDate
        }
    }

    fun updateComments(newComments: List<Comment>) {
        comments.clear()
        comments.addAll(newComments)
        notifyDataSetChanged()
    }
} 