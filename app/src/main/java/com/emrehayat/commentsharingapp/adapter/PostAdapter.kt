package com.emrehayat.commentsharingapp.adapter

import android.icu.text.SimpleDateFormat
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.emrehayat.commentsharingapp.databinding.RecyclerRowBinding
import com.emrehayat.commentsharingapp.model.Post
import com.squareup.picasso.Picasso
import java.util.Locale

class PostAdapter(private val postList : ArrayList<Post>) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    class PostViewHolder(val binding : RecyclerRowBinding) : RecyclerView.ViewHolder(binding.root) {

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = RecyclerRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return postList.size
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.binding.kullaniciAdiRecyclerView.text = postList[position].userName
        holder.binding.yorumRecyclerView.text = postList[position].comment

        val date = postList[position].date.toDate() // Firestore Timestamp'ten Date'e çevirme
        val formattedDate = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(date)
        holder.binding.tarihRecyclerView.text = formattedDate

        Picasso.get().load(postList[position].downloadUrl).into(holder.binding.imageView2)
    }
}