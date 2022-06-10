package com.shannon.music

import android.content.ComponentName
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.AppCompatSeekBar
import com.shannon.library.music.media.*

class MainActivity : AppCompatActivity(), MediaPlayerListener,
    MusicServiceConnection.ConnectionChangeListener {
    val mediaSources = arrayListOf(
        "https://storage.googleapis.com/uamp/The_Kyoto_Connection_-_Wake_Up/01_-_Intro_-_The_Way_Of_Waking_Up_feat_Alan_Watts.mp3", //90S
        "https://storage.googleapis.com/uamp/Kai_Engel_-_Irsens_Tale/01_-_Intro_udonthear.mp3", //63S
        "https://storage.googleapis.com/uamp/Kai_Engel_-_Irsens_Tale/09_-_Outro.mp3" //65S
    )
    private lateinit var serviceConnection: MusicServiceConnection
    private lateinit var titleView: TextView

    private lateinit var previousView: TextView
    private lateinit var playView: TextView
    private lateinit var pauseView: TextView
    private lateinit var nextView: TextView
    private lateinit var playModeView: TextView
    private lateinit var seekBarView: AppCompatSeekBar


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        titleView = findViewById(R.id.titleView)

        previousView = findViewById(R.id.previousView)
        playView = findViewById(R.id.playView)
        pauseView = findViewById(R.id.pauseView)
        nextView = findViewById(R.id.nextView)
        playModeView = findViewById(R.id.playModeView)
        seekBarView = findViewById(R.id.seekBarView)

        serviceConnection =
            MusicServiceConnection(this, ComponentName(this, MusicService::class.java))
        serviceConnection.setConnectionChangeListener(this)
        serviceConnection.connect()

        previousView.setOnClickListener {
            if (serviceConnection.isConnected()) {
                serviceConnection.mediaController?.apply {
                    seekToPreviousItem()
                }
            }
        }
        playView.setOnClickListener {
            if (serviceConnection.isConnected()) {
                serviceConnection.mediaController?.apply {
                    play()
                }
            }
        }
        pauseView.setOnClickListener {
            if (serviceConnection.isConnected()) {
                serviceConnection.mediaController?.apply {
                    pause()
                }
            }
        }
        nextView.setOnClickListener {
            if (serviceConnection.isConnected()) {
                serviceConnection.mediaController?.apply {
                    seekToNextItem()
                }
            }
        }

        playModeView.setOnClickListener {
            if (serviceConnection.isConnected()) {
                serviceConnection.mediaController?.apply {
                    if (switchPlayMode() == MediaController.PLAY_MODE_SHUFFLE) {
                        playModeView.text = "随机"
                    } else {
                        playModeView.text = "顺序"
                    }
                }
            }
        }
    }

    fun onSpeedClick(v: View) {
        val speed = when (v.id) {
            R.id.speedView1 -> 0.5f
            R.id.speedView2 -> 0.75f
            R.id.speedView3 -> 1.0f
            R.id.speedView4 -> 1.25f
            R.id.speedView5 -> 1.5f
            R.id.speedView6 -> 2.0f
            else -> 1.0f
        }
        if (serviceConnection.isConnected()) {
            serviceConnection.mediaController?.apply {
                setPlaybackSpeed(speed)
            }
        }
    }


    override fun onMediaItemTransition(mediaId: String) {
        titleView.text = "正在播放：$mediaId"
    }

    override fun onMediaItemTransition(hasPreviousItem: Boolean, hasNextItem: Boolean) {
        previousView.visibility = if (hasPreviousItem) View.VISIBLE else View.INVISIBLE
        nextView.visibility = if (hasNextItem) View.VISIBLE else View.INVISIBLE

        Log.e(
            "MainA",
            "onMediaItemTransition: hasPreviousItem = $hasPreviousItem ; hasNextItem = $hasNextItem"
        )
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        if (isPlaying) {
            pauseView.visibility = View.VISIBLE
            playView.visibility = View.INVISIBLE
        } else {
            pauseView.visibility = View.INVISIBLE
            playView.visibility = View.VISIBLE
        }
    }

    override fun onIsLoadingChanged(isLoading: Boolean) {
        playView.isEnabled = !isLoading
    }

    override fun updateProgress(positionMs: Long, duration: Long) {
        seekBarView.max = duration.toInt()
        seekBarView.progress = positionMs.toInt()
    }

    override fun onConnectionChanged(state: Int) {
        if (state == MusicServiceConnection.CONNECT_STATE_CONNECTED) {
            serviceConnection.setMediaPlayerListener(this)
            serviceConnection.mediaController?.addMediaSource(
                arrayListOf(
                    MediaData(
                        "晚风心里吹",
                        "https://aod.cos.tx.xmcdn.com/storages/def0-audiofreehighqps/F8/6B/GKwRIJIGb_kQABpfMQFkPhbr.mp3"
                    ),
                    MediaData(
                        "最远的你是我最近的爱",
                        "https://aod.cos.tx.xmcdn.com/storages/e31f-audiofreehighqps/D7/0B/GKwRIJIGXRDeACH1OwFYV3kx.mp3"
                    ),
                    MediaData(
                        "下雨的时候",
                        "https://aod.cos.tx.xmcdn.com/storages/3a60-audiofreehighqps/35/0A/GKwRIMAGTaVfACMb7QFOIC0y.mp3"
                    ),
                    MediaData(
                        "月满西楼",
                        "https://aod.cos.tx.xmcdn.com/storages/9fb5-audiofreehighqps/1A/BA/GKwRINsGMcazAB6adQE99S_T.mp3"
                    ),
                    MediaData(
                        "英雄的黎明",
                        "https://aod.cos.tx.xmcdn.com/storages/fe33-audiofreehighqps/6F/54/GKwRIJEGJMSrABsppAE3RE2L.mp3"
                    ),
                    MediaData(
                        "人世间",
                        "https://aod.cos.tx.xmcdn.com/storages/d72e-audiofreehighqps/67/2E/GKwRIJIGAh7ZAB7s6AEoxaV2.mp3"
                    ),
                    MediaData(
                        "孤勇者",
                        "https://aod.cos.tx.xmcdn.com/storages/9212-audiofreehighqps/F0/F8/GKwRIW4F-UzXAB_NZQEj7LDf.mp3"
                    ),
                    MediaData(
                        "梅香如故",
                        "https://aod.cos.tx.xmcdn.com/storages/898f-audiofreehighqps/34/E7/GKwRIJIF6BLtAB3UFwEZ-BGb.mp3"
                    ),
                    MediaData(
                        "偶然",
                        "https://aod.cos.tx.xmcdn.com/storages/9beb-audiofreehighqps/64/CE/GKwRIRwF1ZAcABOI_QEUbJVC.mp3"
                    ),
                    MediaData(
                        "幻昼",
                        "https://aod.cos.tx.xmcdn.com/storages/8e4e-audiofreehighqps/FF/36/GKwRIJEFxcL6AB_fWwERKx2u.mp3"
                    ),
                    MediaData(
                        "最爱",
                        "https://aod.cos.tx.xmcdn.com/storages/2c9e-audiofreehighqps/D8/5E/GKwRINsFrQUCACHfMQEKvcI4.mp3"
                    ),
                    MediaData(
                        "慢慢喜欢你",
                        "https://aod.cos.tx.xmcdn.com/group50/M05/84/74/wKgKmVvB3VvzJzksABrR4t_qs5A584.mp3"
                    ),
                    MediaData(
                        "千千阙歌",
                        "https://aod.cos.tx.xmcdn.com/storages/014f-audiofreehighqps/9E/B7/GKwRIDoFZtuMACZ9OwD3RIqE.mp3"
                    ),
                    MediaData(
                        "胡广生",
                        "https://aod.cos.tx.xmcdn.com/group62/M06/6B/04/wKgMcVz8o7iQKf0RABtzvolTvB0240.mp3"
                    ),
                    MediaData(
                        "卡农",
                        "https://aod.cos.tx.xmcdn.com/group28/M04/1B/3F/wKgJSFlSd7WiGqn_AB4Ni2yg1Nk609.mp3"
                    ),
                    MediaData(
                        "此情不移",
                        "https://aod.cos.tx.xmcdn.com/storages/358a-audiofreehighqps/34/9A/GKwRIDoFnQ_VAB8SdQEG1EmZ.mp3"
                    ),
                    MediaData(
                        "一生所爱",
                        "https://aod.cos.tx.xmcdn.com/group28/M03/AF/BE/wKgJSFkq75Ox47PTACGQLJjGc7Q890.mp3"
                    ),
                    MediaData(
                        "飘摇",
                        "https://aod.cos.tx.xmcdn.com/storages/ae60-audiofreehighqps/F4/DE/CKwRIMAFK2miAB3UFwDoL0di.mp3"
                    )
                )
            )
        }
    }
}