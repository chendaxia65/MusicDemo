package com.shannon.library.music.media

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log

/**
 *
 * @ProjectName:    MusicDemo
 * @Package:        com.shannon.library.music.media
 * @ClassName:      MusicServiceConnection
 * @Description:     java类作用描述
 * @Author:         czhen
 * @CreateDate:     2022/6/8 10:30
 */
class MusicServiceConnection(
    private val context: Context,
    private val componentName: ComponentName
) {

    @Volatile
    private var mState = CONNECT_STATE_DISCONNECTED

    var mediaController: MediaController? = null
        private set

    private var mServiceConnection: InnerServiceConnection? = null
    private var connectionChangeListener: ConnectionChangeListener? = null

    fun connect() {
        if (mState != CONNECT_STATE_DISCONNECTING && mState != CONNECT_STATE_DISCONNECTED) {
            Log.e(TAG, "connect() called while neither disconnecting nor disconnected ")
            return
        }
        setState(CONNECT_STATE_CONNECTING)

        mServiceConnection = InnerServiceConnection()
        val intent = Intent()
        intent.component = componentName
        var bound = false
        try {
            bound = context.bindService(intent, mServiceConnection!!, Context.BIND_AUTO_CREATE)
        } catch (ex: Exception) {
            Log.e(TAG, "Failed binding to service $mServiceConnection")
        }

        if (!bound) {
            forceCloseConnection()
        }
    }

    fun setMediaPlayerListener(mediaPlayerListener: MediaPlayerListener) {
        if (isConnected()) {
            mediaController?.setMediaPlayerListener(mediaPlayerListener)
            Log.e(TAG, "setMediaPlayerListener: ")
        }
    }

    fun disconnect() {
        setState(CONNECT_STATE_DISCONNECTING)
        forceCloseConnection()
    }

    fun setConnectionChangeListener(connectionChangeListener: ConnectionChangeListener) {
        this.connectionChangeListener = connectionChangeListener
    }

    private fun setState(state: Int) {
        mState = state
        connectionChangeListener?.onConnectionChanged(state)
    }

    private inner class InnerServiceConnection : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val serviceBinder = binder as MusicService.ServiceBinder
            mediaController = serviceBinder.getMediaController()
            val mediaControllerCompat =
                MediaControllerCompat(context, serviceBinder.getMediaSessionToken()).apply {
                    registerCallback(MediaControllerCallback())
                }
            setState(CONNECT_STATE_CONNECTED)
            Log.d(TAG, "onServiceConnected: CONNECT_STATE_CONNECTED")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            setState(CONNECT_STATE_DISCONNECTED)
            mediaController = null
        }
    }

    private fun forceCloseConnection() {
        if (mServiceConnection != null) {
            try {
                context.unbindService(mServiceConnection!!)
            } catch (e: IllegalArgumentException) {
                Log.d(TAG, "unbindService failed", e)
            }
        }
        setState(CONNECT_STATE_DISCONNECTED)
        mServiceConnection = null
    }

    fun isConnected() = mState == CONNECT_STATE_CONNECTED

    companion object {
        private const val TAG = "MusicServiceConnection"
        const val CONNECT_STATE_DISCONNECTING = 0
        const val CONNECT_STATE_DISCONNECTED = 1
        const val CONNECT_STATE_CONNECTING = 2
        const val CONNECT_STATE_CONNECTED = 3
        const val CONNECT_STATE_SUSPENDED = 4
    }

    interface ConnectionChangeListener {

        fun onConnectionChanged(state: Int)
    }

    private inner class MediaControllerCallback : MediaControllerCompat.Callback() {

        override fun onPlaybackStateChanged(playbackStateCompat: PlaybackStateCompat?) {
            playbackStateCompat?.apply {
                Log.d(TAG, "onPlaybackStateChanged: state = $state ")
            }
            Log.d(TAG, "onPlaybackStateChanged: state  ")

        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            metadata?.apply {
                val mediaId = getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)
                val duration = getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
                Log.d(TAG, "onMetadataChanged: mediaId = $mediaId ; duration = $duration")
            }
            Log.d(TAG, "onMetadataChanged: mediaId =  ; duration =  ")
        }
    }
}