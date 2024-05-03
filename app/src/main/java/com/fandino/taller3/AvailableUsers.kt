package com.fandino.taller3


import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView.*
import com.google.android.material.floatingactionbutton.FloatingActionButton

class AvailableUsers (pContext: Context, pActiveUsers: ArrayList<User>, pImgMaps : ArrayList <Bitmap>) : Adapter<AvailableUsers.MyViewHolder>()
{
    var context : Context
    var UsersActivos : ArrayList<User>
    var profilePicBitmaps : ArrayList<Bitmap>

    init
    {
        this.context = pContext
        this.UsersActivos = pActiveUsers
        this.profilePicBitmaps = pImgMaps
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder
    {
        val inflater : LayoutInflater = LayoutInflater.from(context)
        val view : View = inflater.inflate(R.layout.available_user_adapter, parent, false)

        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int)
    {
        holder.nameTxtView.text = UsersActivos[position].name.plus(" ").plus(UsersActivos[position].lastName)
        holder.profileImg.setImageBitmap(profilePicBitmaps[position])
        holder.locationBtn.setOnClickListener {
        }
    }

    override fun getItemCount(): Int
    {
        return 1;
    }


    class MyViewHolder(itemView: View) : ViewHolder(itemView)
    {
        val profileImg : ImageView = itemView.findViewById(R.id.profileImg)
        val nameTxtView : TextView = itemView.findViewById(R.id.name)
        val locationBtn : FloatingActionButton = itemView.findViewById(R.id.userLocationBtn)
    }
}