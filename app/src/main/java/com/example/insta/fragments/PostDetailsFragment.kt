package com.example.insta.fragments

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.insta.Adapter.PostAdapter
import com.example.insta.Model.Post
import com.example.insta.R
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [PostDetailsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class PostDetailsFragment : Fragment() {
    private var postAdapter: PostAdapter? = null
    private var postList: MutableList<Post>? = null
    private var postId: String = ""
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
        val view =  inflater.inflate(R.layout.fragment_post_details, container, false)

        val preferences = context?.getSharedPreferences("PREFS", Context.MODE_PRIVATE)
        if (preferences != null )
        {
            postId = preferences.getString("postId", "none").toString()
        }

        var recyclerView: RecyclerView
        recyclerView = view.findViewById(R.id.recycler_view_post_details)
        recyclerView.setHasFixedSize(true)
        val linearLayoutManager = LinearLayoutManager(context)
        recyclerView.layoutManager = linearLayoutManager

        postList  = ArrayList()
        postAdapter = context?.let { PostAdapter(it, postList as ArrayList<Post>) }
        recyclerView.adapter = postAdapter

        retrievePosts()
        return view
    }

    private fun retrievePosts() {
        val postsRef = FirebaseDatabase.getInstance().reference.child("Posts")
            .child(postId)
        postsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                postList?.clear()
                val post = snapshot.getValue(Post::class.java)
                postList!!.add(post!!)
                postAdapter!!.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }


    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment PostDetailsFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            PostDetailsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}