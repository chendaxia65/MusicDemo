package com.shannon.library.music.media

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.KeyEvent
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.ShuffleOrder
import com.google.android.exoplayer2.ui.StyledPlayerControlView
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.util.Util
import com.shannon.library.music.media.extensions.isSkipToNextEnabled
import com.shannon.library.music.media.extensions.isSkipToPreviousEnabled

/**
 *
 * @ProjectName:    MusicDemo
 * @Package:        com.shannon.library.music.media
 * @ClassName:      MusicService
 * @Description:     java类作用描述
 * @Author:         czhen
 * @CreateDate:     2022/6/8 9:43
 */
class MusicService : Service(), MediaController, MediaSessionConnector.MediaButtonEventHandler {

    private val musicAudioAttributes = AudioAttributes.Builder()
        .setContentType(C.CONTENT_TYPE_MUSIC)
        .setUsage(C.USAGE_MEDIA)
        .build()

    private val musicPlayListener = MusicPlayListener()

    private val exoPlayer: ExoPlayer by lazy {
        ExoPlayer.Builder(this).build().apply {
            setAudioAttributes(musicAudioAttributes, true)
            setWakeMode(C.WAKE_MODE_NETWORK)
            setHandleAudioBecomingNoisy(true)
            addListener(musicPlayListener)
            shuffleModeEnabled = false
            setShuffleOrder(ShuffleOrder.DefaultShuffleOrder(0))
        }
    }
    private val dataSourceFactory: DataSource.Factory by lazy { DefaultDataSource.Factory(this) }

    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var mediaSessionConnector: MediaSessionConnector

    private var mediaPlayerListener: MediaPlayerListener? = null
    private lateinit var serviceHandler: Handler
    override fun onCreate() {
        super.onCreate()

        serviceHandler = Handler(mainLooper)
        mediaSession = MediaSessionCompat(this, TAG)
        mediaSession.setSessionActivity(getSessionActivityPendingIntent())
        mediaSession.isActive = true

        mediaSessionConnector = MediaSessionConnector(mediaSession)
        mediaSessionConnector.setMediaButtonEventHandler(this)
        mediaSessionConnector.setPlayer(exoPlayer)

//        mediaSession.controller.registerCallback(MediaControllerCallback())
    }

    override fun play() {
        when (getMediaSessionPlaybackState()) {
            PlaybackStateCompat.STATE_STOPPED,
            PlaybackStateCompat.STATE_NONE -> {
                val firstWindowIndexPlayer =
                    exoPlayer.currentTimeline.getFirstWindowIndex(exoPlayer.shuffleModeEnabled)
                Log.e(TAG, "play: firstWindowIndexPlayer = $firstWindowIndexPlayer")
                play(firstWindowIndexPlayer)
            }
            PlaybackStateCompat.STATE_PAUSED -> {
                exoPlayer.play()
            }
        }
    }

    override fun play(targetPosition: Int) {
        play(targetPosition, 0)
    }

    override fun play(targetPosition: Int, positionMs: Long) {
        exoPlayer.seekTo(targetPosition, positionMs)
        exoPlayer.play()
    }

    override fun pause() {
        exoPlayer.pause()
    }

    override fun stop() {
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
    }

    override fun seekToPreviousItem() {
        val previousWindowIndex = exoPlayer.currentTimeline.getPreviousWindowIndex(
            exoPlayer.currentMediaItemIndex, exoPlayer.repeatMode, exoPlayer.shuffleModeEnabled
        )
        Log.e(TAG, "seekToPreviousItem: previousWindowIndex = $previousWindowIndex")
        exoPlayer.seekToPreviousMediaItem()

        val canToPlay = !exoPlayer.playWhenReady && exoPlayer.hasPreviousMediaItem()
        if (canToPlay) exoPlayer.play()
    }

    override fun seekToNextItem() {
        val nextWindowIndex = exoPlayer.currentTimeline.getNextWindowIndex(
            exoPlayer.currentMediaItemIndex, exoPlayer.repeatMode, exoPlayer.shuffleModeEnabled
        )
        Log.e(TAG, "seekToPreviousItem: nextWindowIndex = $nextWindowIndex")

        exoPlayer.seekToNextMediaItem()
        val canToPlay = !exoPlayer.playWhenReady && exoPlayer.hasNextMediaItem()
        if (canToPlay) exoPlayer.play()
    }

    override fun addMediaSource(sources: List<MediaData>) {
        val concatenatingMediaSource = ConcatenatingMediaSource()
        sources.forEach {
            val mediaItem = MediaItem.Builder()
                .setMediaId(it.mediaId)
                .setUri(it.mediaUri)
                .build()
            val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(mediaItem)

//            concatenatingMediaSource.addMediaSource(mediaSource)
            exoPlayer.addMediaSource(mediaSource)
        }
        exoPlayer.prepare()

    }

    override fun setMediaPlayerListener(mediaPlayerListener: MediaPlayerListener) {
        this.mediaPlayerListener = mediaPlayerListener
    }

    override fun getCurrentMediaItem(): String {
        return exoPlayer.currentMediaItem?.mediaId ?: ""
    }

    override fun switchPlayMode(): @MediaController.PlayMode Int {
        val shuffleModeEnabled = exoPlayer.shuffleModeEnabled
        exoPlayer.shuffleModeEnabled = !shuffleModeEnabled
        mediaPlayerListener?.onMediaItemTransition(
            exoPlayer.hasPreviousMediaItem(),
            exoPlayer.hasNextMediaItem()
        )
        return if (exoPlayer.shuffleModeEnabled) MediaController.PLAY_MODE_SHUFFLE else MediaController.PLAY_MODE_ORDER
    }

    override fun setPlaybackSpeed(speed: Float) {
        val playbackParameters = PlaybackParameters(speed,speed)
        exoPlayer.playbackParameters = playbackParameters
    }

    private fun getSessionActivityPendingIntent(): PendingIntent? {
        val intent = packageManager?.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE)
        }
        return intent
    }

    override fun onBind(intent: Intent?): IBinder {
        return ServiceBinder()
    }

    inner class ServiceBinder : Binder() {
        fun getMediaController(): MediaController {
            return this@MusicService
        }

        fun getMediaSessionToken(): MediaSessionCompat.Token {
            return mediaSession.sessionToken
        }
    }

    private inner class MusicPlayListener : Player.Listener {

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val mediaId = mediaItem?.mediaId ?: ""

            val previousWindowIndex = exoPlayer.currentTimeline.getPreviousWindowIndex(
                exoPlayer.currentMediaItemIndex, exoPlayer.repeatMode, exoPlayer.shuffleModeEnabled
            )
            val nextWindowIndex = exoPlayer.currentTimeline.getNextWindowIndex(
                exoPlayer.currentMediaItemIndex, exoPlayer.repeatMode, exoPlayer.shuffleModeEnabled
            )
            val firstWindowIndexPlayer =
                exoPlayer.currentTimeline.getFirstWindowIndex(exoPlayer.shuffleModeEnabled)
            val lastWindowIndexPlayer =
                exoPlayer.currentTimeline.getLastWindowIndex(exoPlayer.shuffleModeEnabled)
            Log.e(
                TAG,
                "firstWindowIndexPlayer = $firstWindowIndexPlayer ; lastWindowIndexPlayer = $lastWindowIndexPlayer ;${exoPlayer.currentTimeline.javaClass.name}"
            )
            Log.d(
                TAG,
                "onMediaItemTransition: mediaItem.uri = ${mediaItem?.localConfiguration?.uri} ; reason = $reason ; previousWindowIndex = $previousWindowIndex ; nextWindowIndex = $nextWindowIndex"
            )

            mediaPlayerListener?.onMediaItemTransition(mediaId)
            mediaPlayerListener?.onMediaItemTransition(
                exoPlayer.hasPreviousMediaItem(),
                exoPlayer.hasNextMediaItem()
            )
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            Log.d(TAG, "onPlayWhenReadyChanged: playWhenReady = $playWhenReady ; reason = $reason")

        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            Log.d(TAG, "onPlaybackStateChanged: playbackState = $playbackState ")
            if (playbackState == Player.STATE_BUFFERING || playbackState == Player.STATE_READY) {
                mediaPlayerListener?.onMediaItemTransition(
                    exoPlayer.hasPreviousMediaItem(),
                    exoPlayer.hasNextMediaItem()
                )
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            Log.e(TAG, "onPlayerError: ", error)
            exoPlayer.prepare()
        }

        override fun onEvents(player: Player, events: Player.Events) {
            if (events.containsAny(
                    Player.EVENT_POSITION_DISCONTINUITY,
                    Player.EVENT_TIMELINE_CHANGED
                )
            ) {
                updateProgress()
                Log.d(TAG, "onPositionDiscontinuity: reason = ${player.currentPosition} ")
            }
        }

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            Log.d(TAG, "onMediaMetadataChanged: mediaMetadata = $mediaMetadata ")
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            Log.d(TAG, "onIsPlayingChanged: isPlaying = $isPlaying ")
            mediaPlayerListener?.onIsPlayingChanged(isPlaying)
        }

        override fun onIsLoadingChanged(isLoading: Boolean) {
            if (isLoading && getMediaSessionPlaybackState() == PlaybackStateCompat.STATE_PLAYING) return

            mediaPlayerListener?.onIsLoadingChanged(isLoading)
            Log.d(
                TAG,
                "onIsLoadingChanged: isLoading = $isLoading ; playbackState = ${getMediaSessionPlaybackStateText()}"
            )
        }


    }

    companion object {
        private const val TAG = "MusicService"
        private const val MAX_UPDATE_INTERVAL_MS = 1000L
        private const val MIN_UPDATE_INTERVAL_MS = 200L

    }

    override fun onMediaButtonEvent(player: Player, mediaButtonEvent: Intent): Boolean {
        val keyEvent = mediaButtonEvent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)
        if (keyEvent?.action == KeyEvent.ACTION_UP) {
            when (keyEvent.keyCode) {
                KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                    seekToPreviousItem()
                    Log.d(TAG, "KEYCODE_MEDIA_PREVIOUS")
                }
                KeyEvent.KEYCODE_MEDIA_NEXT -> {
                    seekToNextItem()
                    Log.d(TAG, "KEYCODE_MEDIA_NEXT")
                }
            }
        }
        return true
    }

    private fun getMediaSessionPlaybackState(): Int {
        val playWhenReady = exoPlayer.playWhenReady
        return when (exoPlayer.playbackState) {
            Player.STATE_BUFFERING -> if (playWhenReady) PlaybackStateCompat.STATE_BUFFERING else PlaybackStateCompat.STATE_PAUSED
            Player.STATE_READY -> if (playWhenReady) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
            Player.STATE_ENDED -> PlaybackStateCompat.STATE_STOPPED
            Player.STATE_IDLE -> PlaybackStateCompat.STATE_NONE
            else -> PlaybackStateCompat.STATE_NONE
        }
    }

    private fun getMediaSessionPlaybackStateText(): String {
        return when (getMediaSessionPlaybackState()) {
            PlaybackStateCompat.STATE_BUFFERING -> "STATE_BUFFERING"
            PlaybackStateCompat.STATE_PAUSED -> "STATE_PAUSED"
            PlaybackStateCompat.STATE_PLAYING -> "STATE_PLAYING"
            PlaybackStateCompat.STATE_STOPPED -> "STATE_STOPPED"
            PlaybackStateCompat.STATE_NONE -> "STATE_NONE"
            else -> "NULL"
        }
    }

    private fun updateProgress() {

        val positionMs = exoPlayer.contentPosition
        val duration = exoPlayer.duration
        mediaPlayerListener?.updateProgress(positionMs, duration)

//        Log.d(TAG, "updateProgress: position = $positionMs ; duration = $duration")
        serviceHandler.removeCallbacks(updateProgressAction)
        val playbackState = exoPlayer.playbackState
        if (exoPlayer.isPlaying) {
            val mediaTimeUntilNextFullSecondMs = 1000 - positionMs % 1000

            val playbackSpeed = exoPlayer.playbackParameters.speed
            var delayMs =
                if (playbackSpeed > 0) (mediaTimeUntilNextFullSecondMs / playbackSpeed).toLong() else 1000L
            delayMs = Util.constrainValue(delayMs, MIN_UPDATE_INTERVAL_MS, MAX_UPDATE_INTERVAL_MS)
//            Log.d(TAG, "updateProgress: delayMs = $delayMs ")

            serviceHandler.postDelayed(updateProgressAction, delayMs)
        } else if (playbackState != Player.STATE_ENDED && playbackState != Player.STATE_IDLE) {
            serviceHandler.postDelayed(updateProgressAction, MAX_UPDATE_INTERVAL_MS)

        }
    }

    private val updateProgressAction: Runnable = Runnable {
        updateProgress()
    }

    private inner class MediaControllerCallback : MediaControllerCompat.Callback() {

        override fun onSessionReady() {
            Log.d(TAG, "onSessionReady")
        }

        override fun onPlaybackStateChanged(playbackStateCompat: PlaybackStateCompat?) {
            playbackStateCompat?.apply {
                Log.d(TAG, "onPlaybackStateChanged: state = $state ")

                exoPlayer.currentPosition
                Log.e(
                    TAG,
                    "onMediaItemTransition: hasPreviousItem = $isSkipToPreviousEnabled ; hasNextItem = $isSkipToNextEnabled"
                )
            }

        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            metadata?.apply {
                val mediaId = getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)
                val duration = getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
                Log.d(TAG, "onMetadataChanged: mediaId = $mediaId ; duration = $duration")
            }
        }
    }
}