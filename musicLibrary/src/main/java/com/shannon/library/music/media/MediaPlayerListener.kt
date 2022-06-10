package com.shannon.library.music.media

/**
 *
 * @ProjectName:    MusicDemo
 * @Package:        com.shannon.library.music.media
 * @ClassName:      MediaPlayerListener
 * @Description:     java类作用描述
 * @Author:         czhen
 * @CreateDate:     2022/6/8 15:43
 */
interface MediaPlayerListener {

    fun onMediaItemTransition(mediaId: String)

    fun onMediaItemTransition(hasPreviousItem: Boolean, hasNextItem: Boolean)

    fun onIsPlayingChanged(isPlaying: Boolean)

    fun onIsLoadingChanged(isLoading: Boolean)

    fun updateProgress(positionMs: Long, duration: Long)
}