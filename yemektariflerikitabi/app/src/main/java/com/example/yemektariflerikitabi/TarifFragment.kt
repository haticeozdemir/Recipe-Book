package com.example.yemektariflerikitabi

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.content.ContextCompat
import androidx.navigation.Navigation
import kotlinx.android.synthetic.main.fragment_tarif.*
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.lang.Exception


class TarifFragment : Fragment() {
    var secilenGorsel : Uri? =null
    var secilenBitmap: Bitmap? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)



    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_tarif, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        button.setOnClickListener(){
            kaydet(it)
        }
        imageView.setOnClickListener(){
            gorselSec(it)
        }
        arguments?.let {
            var gelenbilgi = TarifFragmentArgs.fromBundle(it).bilgi
            if (gelenbilgi.equals("menudengeldim")) {
                //yeni yemek eklemeye geldi
                yemekismiText.setText("")
                yemekMalzemelerText.setText("")
                button.visibility = View.VISIBLE

                val gorselsecmeArkaPlani = BitmapFactory.decodeResource(context?.resources,R.drawable.gorsel)
                imageView.setImageBitmap(gorselsecmeArkaPlani)


            }else{
                //daha önce olusturulan yemeğe bakmaya geldi
                button.visibility = View.VISIBLE
                val secilenid = TarifFragmentArgs.fromBundle(it).id
                context?.let{
                    try {
                        val db = it.openOrCreateDatabase("Yemekler",Context.MODE_PRIVATE,null)
                        val cursor = db.rawQuery("SELECT * FROM yemekler WHERE id =? ", arrayOf(secilenid.toString()))
                        val yemekIsmiIndex= cursor.getColumnIndex("yemekismi")
                        val yemekMalzemeIndex= cursor.getColumnIndex("yemekmalzemesi")
                        val yemekGorseli = cursor.getColumnIndex("gorsel")
                        while(cursor.moveToNext()){
                            yemekismiText.setText(cursor.getString(yemekIsmiIndex))
                            yemekMalzemelerText.setText(cursor.getString(yemekMalzemeIndex))
                            val byteDizisi = cursor.getBlob(yemekGorseli)
                            val bitmap= BitmapFactory.decodeByteArray(byteDizisi,0,byteDizisi.size)
                            imageView.setImageBitmap(bitmap)
                        }
                        cursor.close()

                    }catch (e: Exception){

                    }

                }


            }
        }

    }
    fun kaydet(view: View){
        var yemekismi = yemekismiText.text.toString()
        var yemekMalzemeleri = yemekMalzemelerText.text.toString()

        if(secilenBitmap != null){
            val kucukBitmap = kucukBitmapOlustur(secilenBitmap!!,maximumBoyut = 300)
            val outputStream = ByteArrayOutputStream()
            kucukBitmap.compress(Bitmap.CompressFormat.PNG, 50,outputStream)
            val byteDizisi = outputStream.toByteArray()

            try {
                context?.let {
                    val database = it.openOrCreateDatabase("Yemekler", Context.MODE_PRIVATE,null)
                    database.execSQL("CREATE TABLE IF NOT EXISTS yemekler (id INTEGER PRIMARY KEY,yemekismi VARCHAR, yemekmalzemesi VARCHAR, gorsel BLOB)")

                    val sqlString = "INSERT INTO yemekler (yemekismi, yemekmalzemesi, gorsel) VALUES(?,?,?)"
                    val statement= database.compileStatement(sqlString)
                    statement.bindString(1,yemekismi)
                    statement.bindString(2,yemekMalzemeleri)
                    statement.bindBlob(3,byteDizisi)
                    statement.execute()
                }


            }catch (e: Exception){
                e.printStackTrace()

            }
            val action= TarifFragmentDirections.actionTarifFragmentToListeFragment()
            Navigation.findNavController(view).navigate(action)

        }


    }
    fun gorselSec(view: View){
        activity?.let{
        if(ContextCompat.checkSelfPermission(it.applicationContext,Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            //izin verilmedi izin iste
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),1)
        }else{
            //izin zaten verilmiş
            val galeriIntent = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(galeriIntent,2)
        } }


    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode==1){
            if(grantResults.size>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                //izin alındı
                val galeriIntent = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(galeriIntent,2)

            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        //galeriye gidince ne olacak
        if(requestCode == 2 && resultCode == Activity.RESULT_OK && data != null ){
            secilenGorsel = data.data
            try {
                context?.let{
                    if(secilenGorsel != null ){
                        if(Build.VERSION.SDK_INT >= 28){
                            val source = ImageDecoder.createSource(it.contentResolver,secilenGorsel!!)
                            secilenBitmap = ImageDecoder.decodeBitmap(source)
                            imageView.setImageBitmap(secilenBitmap)
                        }else {
                            secilenBitmap = MediaStore.Images.Media.getBitmap(it.contentResolver,secilenGorsel)
                            imageView.setImageBitmap(secilenBitmap)

                        }

                    }
                }



            } catch (e: Exception){
                e.printStackTrace()
            }

        }
        super.onActivityResult(requestCode, resultCode, data)
    }
    fun kucukBitmapOlustur(kullanicininSectigiBitmap: Bitmap, maximumBoyut :Int): Bitmap {
        var width = kullanicininSectigiBitmap.width
        var height = kullanicininSectigiBitmap.height

        val BitmapOrani : Double = width.toDouble() / height.toDouble()
        if (BitmapOrani >1){
            //gorsel yatay
            width =maximumBoyut
            val kisaltilmisheight = width / BitmapOrani
            height = kisaltilmisheight.toInt()


        }else{
            //gorsel dikey
            height=maximumBoyut
            val kisaltilmiswidth = height * BitmapOrani
            width = kisaltilmiswidth.toInt()

        }

        return Bitmap.createScaledBitmap(kullanicininSectigiBitmap,height,width,true)

    }

}