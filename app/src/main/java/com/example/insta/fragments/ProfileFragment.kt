package com.example.insta.fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.insta.AccountSettingsActivity
import com.example.insta.Adapter.MyImagesAdapter
import com.example.insta.Model.Post
import com.example.insta.Model.User
import com.example.insta.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_profile.*
import kotlinx.android.synthetic.main.fragment_profile.view.*
import java.util.*
import kotlin.collections.ArrayList

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [ProfileFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ProfileFragment : Fragment() {
    private lateinit var profileId: String
    private lateinit var firebaseUser: FirebaseUser

    var postList: List<Post>? = null
    var myImagesAdapter: MyImagesAdapter? = null
    var myImagesAdapterSavedImages: MyImagesAdapter? = null
    var postListSaved: List<Post>? = null
    var mySavesImg: List<String>? = null

    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        firebaseUser = FirebaseAuth.getInstance().currentUser!!
        val pref = context?.getSharedPreferences("PREFS",Context.MODE_PRIVATE)
        if (pref != null)
        {
            this.profileId = pref.getString("profileId","none").toString()
        }
        if (profileId == firebaseUser.uid)
        {
            view.edit_account_settings_btn.text = "Edit Profile"
        }
        else if(profileId != firebaseUser.uid)
        {
            checkFollowAndFollowingButtonStatus()
        }
        //Uploaded images
        var recyclerViewUploadImages: RecyclerView
        recyclerViewUploadImages = view.findViewById(R.id.recycler_view_upload_pic)
        recyclerViewUploadImages.setHasFixedSize(true)
        val linearLayoutManager: LinearLayoutManager = GridLayoutManager(context,3)
        recyclerViewUploadImages.layoutManager = linearLayoutManager

        postList = ArrayList()
        myImagesAdapter = context?.let { MyImagesAdapter(it, postList as ArrayList<Post> ) }
        recyclerViewUploadImages.adapter = myImagesAdapter

        //Saved images
        var recyclerViewSavedImages: RecyclerView
        recyclerViewSavedImages = view.findViewById(R.id.recycler_view_saved_pic)
        recyclerViewSavedImages.setHasFixedSize(true)
        val linearLayoutManager2: LinearLayoutManager = GridLayoutManager(context,3)
        recyclerViewSavedImages.layoutManager = linearLayoutManager2

        postListSaved = ArrayList()
        myImagesAdapterSavedImages = context?.let { MyImagesAdapter(it, postListSaved as ArrayList<Post> ) }
        recyclerViewSavedImages.adapter = myImagesAdapterSavedImages

        recyclerViewSavedImages.visibility = View.GONE
        recyclerViewUploadImages.visibility = View.VISIBLE

        var uploadedImagesBtn: ImageButton
        uploadedImagesBtn = view.findViewById(R.id.images_view_grid_btn)
        uploadedImagesBtn.setOnClickListener {
            recyclerViewSavedImages.visibility = View.GONE
            recyclerViewUploadImages.visibility = View.VISIBLE
        }

        var savedImagesBtn: ImageButton
        savedImagesBtn = view.findViewById(R.id.images_save_btn)
        savedImagesBtn.setOnClickListener {
            recyclerViewSavedImages.visibility = View.VISIBLE
            recyclerViewUploadImages.visibility = View.GONE
        }

        view.edit_account_settings_btn.setOnClickListener {
            val getButtonText = view.edit_account_settings_btn.text.toString()
            when
            {
                getButtonText == "Edit Profile" -> startActivity(Intent(context, AccountSettingsActivity::class.java))
                getButtonText == "Follow" -> {
                    firebaseUser?.uid.let {
                        FirebaseDatabase.getInstance().reference
                            .child("Follow")
                            .child(it.toString()).child("Following").child(profileId)
                            .setValue(true)
                    }
                    firebaseUser?.uid.let {
                        FirebaseDatabase.getInstance().reference
                            .child("Follow")
                            .child(profileId).child("Followers").child(it.toString())
                            .setValue(true)
                    }
                    addNotification()

                }

                getButtonText == "Following" -> {
                    firebaseUser?.uid.let {
                        FirebaseDatabase.getInstance().reference
                            .child("Follow")
                            .child(it.toString()).child("Following").child(profileId)
                            .removeValue()
                    }
                    firebaseUser?.uid.let {
                        FirebaseDatabase.getInstance().reference
                            .child("Follow")
                            .child(profileId).child("Followers").child(it.toString())
                            .removeValue()
                    }

                }
            }
            //
        }
        getFollowers()
        getFollowing()
        userInfo()
        myPhotos()
        getTotalNumberOfPosts()
        mySaves()

        return view
    }

    private fun mySaves() {
        mySavesImg = ArrayList()
        val savedRef = FirebaseDatabase.getInstance().reference.child("Saves")
            .child(firebaseUser.uid)
        savedRef.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists())
                {
                    for (snapshot in snapshot.children)
                    {
                        (mySavesImg as ArrayList<String>).add(snapshot.key!!)
                    }
                    readSavedImagesData()
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    private fun readSavedImagesData() {
        val postsRef = FirebaseDatabase.getInstance().reference.child("Posts")
        postsRef.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists())
                {
                    (postListSaved as ArrayList<Post>).clear()
                    for (snapshot in snapshot.children)
                    {
                        val post = snapshot.getValue(Post::class.java)
                        for (key in mySavesImg!!)
                        {
                            if (post!!.getPostid() == key)
                            {
                                (postListSaved as ArrayList<Post>).add(post!!)
                            }
                        }
                    }
                    myImagesAdapterSavedImages!!.notifyDataSetChanged()
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    private fun checkFollowAndFollowingButtonStatus() {
        val followingRef = firebaseUser?.uid.let {
            FirebaseDatabase.getInstance().reference
                .child("Follow")
                .child(it.toString()).child("Following")
        }
        if (followingRef != null)
        {
            followingRef.addValueEventListener(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.child(profileId).exists())
                    {
                        view?.edit_account_settings_btn?.text = "Following"
                    }
                    else
                    {
                        view?.edit_account_settings_btn?.text = "Follow"
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                }
            })
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment ProfileFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ProfileFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
    private fun getFollowers()
    {
        val followersRef = FirebaseDatabase.getInstance().reference
                .child("Follow")
                .child(profileId).child("Followers")

        followersRef.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists())
                {
                    view?.total_followers?.text = snapshot.childrenCount.toString()
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }
    private fun getFollowing()
    {
        val followersRef = FirebaseDatabase.getInstance().reference
                .child("Follow")
                .child(profileId).child("Following")

        followersRef.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists())
                {
                    view?.total_following?.text = snapshot.childrenCount.toString()
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    private fun myPhotos()
    {
        val postsRef = FirebaseDatabase.getInstance().reference.child("Posts")
        postsRef.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists())
                {
                    (postList as ArrayList<Post>).clear()
                    for (snapshot in snapshot.children)
                    {
                        val post = snapshot.getValue(Post::class.java)!!
                        if (post.getPublisher().equals(profileId))
                        {
                            (postList as ArrayList<Post>).add(post)
                        }
                        Collections.reverse(postList)
                        myImagesAdapter!!.notifyDataSetChanged()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }
    private fun userInfo()
    {
        val  usersRef = FirebaseDatabase.getInstance().getReference().child("Users").child(profileId)
        usersRef.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists())
                {
                    val user = snapshot.getValue<User>(User::class.java)
                    Picasso.get().load(user!!.getImage()).placeholder(R.drawable.profile).into(view?.pro_image_profile_frag)
                    view?.profile_fragment_username?.text = user!!.getUsername()
                    view?.full_name_profile_frag?.text = user!!.getFullname()
                    view?.bio_profile_frag?.text = user!!.getBio()
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    override fun onStop() {
        super.onStop()

        val pref = context?.getSharedPreferences("PREFS",Context.MODE_PRIVATE)?.edit()
        pref?.putString("profileId",firebaseUser.uid)
        pref?.apply()
    }

    override fun onPause() {
        super.onPause()

        val pref = context?.getSharedPreferences("PREFS",Context.MODE_PRIVATE)?.edit()
        pref?.putString("profileId",firebaseUser.uid)
        pref?.apply()
    }

    override fun onDestroy() {
        super.onDestroy()

        val pref = context?.getSharedPreferences("PREFS",Context.MODE_PRIVATE)?.edit()
        pref?.putString("profileId",firebaseUser.uid)
        pref?.apply()
    }

    private fun getTotalNumberOfPosts()
    {
        val postRef = FirebaseDatabase.getInstance().reference.child("Posts")
        postRef.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists())
                {
                    var postCounter = 0
                    for (snapshot in snapshot.children)
                    {
                        val post = snapshot.getValue(Post::class.java)!!
                        if (post.getPublisher() == profileId)
                        {
                            postCounter++
                        }
                    }
                    total_posts.text = " "+ postCounter
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }
    private fun addNotification()
    {
        val notiRef = FirebaseDatabase.getInstance().reference.child("Notifications")
            .child(profileId)
        val notiMap = HashMap<String, Any>()
        notiMap["userid"] = firebaseUser!!.uid
        notiMap["text"] = "Started Following you"
        notiMap["postid"] = ""
        notiMap["ispost"] = false
        notiRef.push().setValue(notiMap)

    }
}