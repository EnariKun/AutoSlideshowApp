package jp.techacademy.manao.autoslideshowapp

import android.Manifest
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import android.content.ContentUris
import android.view.View
import android.support.design.widget.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity(), View.OnClickListener {

    private val PERMISSIONS_REQUEST_CODE = 100
    // 画像IDのための変数
    private var index:Int = 0
    private var id:Long = -1
    private var idList: MutableList<Long> = mutableListOf()
    // タイマー用の時間のための変数
    private var mTimer: Timer? = null
    private var mHandler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Android 6.0以降の場合
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // パーミッションの許可状態を確認する
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                // 許可されている
                getContentsInfo()
            } else {
                // 許可されていないので許可ダイアログを表示する
                requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), PERMISSIONS_REQUEST_CODE)
            }
            // Android 5系以下の場合
        } else {
            getContentsInfo()
        }

        //各ボタンの設定
        prev_button.setOnClickListener(this)
        play_button.setOnClickListener(this)
        next_button.setOnClickListener(this)
    }

    override fun onRestart() {
        super.onRestart()
        //ID格納リストの初期化
        idList = mutableListOf()
        // Android 6.0以降の場合
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // パーミッションの許可状態を確認する
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                // 許可されている
                getContentsInfo()
            } else {
                // 許可されていないので何もしない
            }
            // Android 5系以下の場合
        } else {
            getContentsInfo()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSIONS_REQUEST_CODE ->
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getContentsInfo()
                } else {
                    // アクセス権限が無い場合、noImageTextを表示し、ボタンを全て非活性にする
                    setDisableButton("ストレージへのアクセス権限が無いため、\n画像を取得できませんでした")
                }
        }
    }

    private fun getContentsInfo() {
        // 画像の情報を取得する
        val resolver = contentResolver
        val cursor = resolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, // データの種類
            null, // 項目(null = 全項目)
            null, // フィルタ条件(null = フィルタなし)
            null, // フィルタ用パラメータ
            null // ソート (null ソートなし)
        )

        if (cursor!!.moveToFirst()) {
            setEnableButton()
            // indexからIDを取得し、そのIDから画像のURIを取得する
            idList.add(cursor.getLong(cursor.getColumnIndex(MediaStore.Images.Media._ID)))
            // 全てのIDを取得し、mutableListに格納する
            while (cursor.moveToNext()) {
                idList.add(cursor.getLong(cursor.getColumnIndex(MediaStore.Images.Media._ID)))
            }

            if(id == -1.toLong()){
                //アプリ期起動時
                setImageId()
            } else if(idList.contains(id)){
                //アプリRestart時に開いていた画像が存在する場合
                index = idList.indexOf(id)
                setImageId()
            } else {
                //アプリRestart時に開いていた画像が存在しない場合
                //最後に開いていた画像よりIDが大きい直近の画像を検索
                index = idList.indexOfFirst{it > id}
                //最後に開いていた画像よりIDが小さい直近の画像を検索
                if(index == -1){
                    index = idList.indexOfLast{it < id}
                }
                setImageId()
            }
        } else {
            // 画像が無い場合、noImageTextを表示し、ボタンを全て非活性にする
            setDisableButton("画像が見つかりませんでした")
        }
        cursor.close()
    }

    override fun onClick(v: View) {
        if(idList.size == 1){
            Snackbar.make(v, "画像が1つしかありません", Snackbar.LENGTH_SHORT)
                .show()
        } else {
            when(v.id){
                R.id.next_button -> getNextImage()
                R.id.play_button -> playSlideShow()
                R.id.prev_button -> getPrevImage()
            }
        }
    }
    private fun playSlideShow(){
        if (mTimer == null){
            // 再生ボタンのテキストを変更し、進むボタンと戻るボタンを非活性にする
            play_button.text = "停止"
            prev_button.isEnabled  = false
            next_button.isEnabled  = false
            mTimer = Timer()
            mTimer!!.schedule(object : TimerTask() {
                override fun run() {
                    mHandler.post {
                        getNextImage()
                    }
                }
            }, 2000, 2000) // 最初に始動させるまで 100ミリ秒、ループの間隔を 100ミリ秒 に設定
        } else {
            // 停止ボタンのテキストを変更し、進むボタンと戻るボタンを活性にする
            play_button.text = "再生"
            prev_button.isEnabled  = true
            next_button.isEnabled  = true
            if (mTimer != null){
                mTimer!!.cancel()
                mTimer = null
            }
        }
    }

    private fun getNextImage() {
        // 次の画像を取得する
        if(index + 1 == idList.size){
            index = 0
        } else {
            index++
        }
        setImageId()
    }

    private fun getPrevImage() {
        // 前の画像を取得する
        if(index  == 0){
            index = idList.size - 1
        } else {
            index--
        }
        setImageId()
    }

    private fun setImageId() {
        //indexで指定されている画像を表示
        id = idList[index]
        imageView.setImageURI(ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id))
    }

    private fun setEnableButton() {
        //imageViewを表示し、noImageTextを非表示にし、ボタンを全て活性にする。
        imageView.visibility = View.VISIBLE
        noImageText.text = ""
        noImageText.visibility = View.INVISIBLE
        next_button.isEnabled  = true
        play_button.isEnabled  = true
        prev_button.isEnabled  = true
    }

    private fun setDisableButton(str: String) {
        //imageViewを非表示にし、noImageTextを表示し、ボタンを全て非活性にする。
        imageView.visibility = View.INVISIBLE
        noImageText.text = str
        noImageText.visibility = View.VISIBLE
        next_button.isEnabled  = false
        play_button.isEnabled  = false
        prev_button.isEnabled  = false
    }

}