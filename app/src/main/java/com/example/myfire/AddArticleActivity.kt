package com.example.myfire

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.example.myfire.DBKey.Companion.DB_ARTICLES
import com.example.myfire.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage

class AddArticleActivity : AppCompatActivity() {

    private var selectedUri: Uri? = null
    private val auth: FirebaseAuth by lazy {
        Firebase.auth
    }
    private val storage: FirebaseStorage by lazy {
        Firebase.storage
    }
    private val articleDB: DatabaseReference by lazy {
        Firebase.database.reference.child(DB_ARTICLES)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_article)

        findViewById<Button>(R.id.imageAddButton).setOnClickListener {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED -> {
                    startContentProvider()
                }
                shouldShowRequestPermissionRationale(android.Manifest.permission.READ_EXTERNAL_STORAGE) -> {
                    showPermissionContextPopup()
                }
                else -> {
                    requestPermissions(
                        arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                        1000
                    )
                }
            }
        }

        findViewById<Button>(R.id.submitButton).setOnClickListener {
            val title = findViewById<EditText>(R.id.titleEditText).text.toString()
            val price = findViewById<EditText>(R.id.priceEditText).text.toString()
            val seller = findViewById<EditText>(R.id.sellerEditText).text.toString()
            val content = findViewById<EditText>(R.id.contentEditText).text.toString()
            val isSell = findViewById<CheckBox>(R.id.sellCheckBox).isChecked
            val sellerId = auth.currentUser?.uid.orEmpty()

            showProgressBar()


            if (selectedUri != null) {
                val photoUri = selectedUri ?: return@setOnClickListener
                uploadPhoto(
                    photoUri,
                    succesHandler = { uri ->
                        val seller = findViewById<EditText>(R.id.sellerEditText).text.toString() // 추가
                        val content = findViewById<EditText>(R.id.contentEditText).text.toString()
                        val isSell = findViewById<CheckBox>(R.id.sellCheckBox).isChecked
                        uploadArticle(title, price, sellerId, seller, uri, content, isSell) // 수정
                    },
                    errorHandler = {
                        Toast.makeText(this, "사진 업로드에 실패했습니다.", Toast.LENGTH_SHORT).show()
                        hideProgressBar()
                    }
                )
            } else {
                val seller = findViewById<EditText>(R.id.sellerEditText).text.toString() // 추가
                val content = findViewById<EditText>(R.id.contentEditText).text.toString()
                val isSell = findViewById<CheckBox>(R.id.sellCheckBox).isChecked
                uploadArticle(title, price, sellerId, seller, "", content, isSell) // 수정
            }

        }
    }

    private fun uploadPhoto(uri: Uri, succesHandler: (String) -> Unit, errorHandler: () -> Unit) {
        val fileName = "${System.currentTimeMillis()}.png"

        storage.reference.child("article/photo").child(fileName)
            .putFile(uri)
            .addOnCompleteListener {
                if(it.isSuccessful) {
                    storage.reference.child("article/photo").child(fileName).downloadUrl
                        .addOnSuccessListener { uri ->
                            succesHandler(uri.toString())
                        }
                        .addOnFailureListener {
                            errorHandler()
                        }
                }
                else {
                    errorHandler()
                }
            }
    }

    private fun uploadArticle(
        title: String,
        price: String,
        sellerId: String,
        sellerName: String, // 추가
        imageUrl: String,
        content: String,
        isSell: Boolean
    ) {
        val model = ArticleMddel(
            sellerId,
            sellerName, // 추가
            title,
            content,
            isSell,
            System.currentTimeMillis(),
            "$price 원",
            imageUrl
        )
        articleDB.push().setValue(model)

        hideProgressBar()
        finish()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            1000 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startContentProvider()
                } else {
                    Toast.makeText(this, "권한을 거부하셨습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun startContentProvider() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        startActivityForResult(intent, 2000)
    }

    private fun showProgressBar() {
        findViewById<ProgressBar>(R.id.progressBar).isVisible = true
    }

    private fun hideProgressBar() {
        findViewById<ProgressBar>(R.id.progressBar).isVisible = false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode != Activity.RESULT_OK) {
            return
        }

        when (requestCode) {
            2000 -> {
                val uri = data?.data
                if (uri != null) {
                    findViewById<ImageView>(R.id.photoImageView).setImageURI(uri)
                    selectedUri = uri
                } else {
                    Toast.makeText(this, "사진을 가져오기 못했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            else -> {
                Toast.makeText(this, "사진을 가져오기 못했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showPermissionContextPopup() {
        AlertDialog.Builder(this)
            .setTitle("권한이 필요합니다.")
            .setMessage("사진을 가져오기 위해 필요합니다.")
            .setPositiveButton("동의") { _, _ ->
                requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), 1000)
            }
            .create()
            .show()
    }
}