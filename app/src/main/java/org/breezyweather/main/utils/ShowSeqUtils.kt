package org.breezyweather.main

import android.os.Environment
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date

class ShowSeqUtils {
    private var fr: FileReader? = null
    private var br: BufferedReader? = null
    private var fw: FileWriter? = null
    private val bw: BufferedWriter? = null

    fun loadShowSeq(): Int {
        var show_seq = 1
        //获取当前日期字符串
        val sdf = SimpleDateFormat("yyyy-MM-dd")
        val data = sdf.format(Date())
        //读取本地缓存show_seq的文件
        try {
            val rootPath = Environment.getExternalStorageDirectory().path
            val file = File("$rootPath/Android/data/com.snssdk.api/cache/adloadSeqTemp.txt")
            if (!file.exists()) {
                file.createNewFile()
                return show_seq
            }
            fr = FileReader(file)
            br = BufferedReader(fr)
            var line = ""
            while ((br!!.readLine().also { line = it }) != null) {
                val temp = line.split(",".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
                if (temp[0] == data) {
                    //日期相同返回字段
                    show_seq = temp[1].toInt()
                }
            }
            return show_seq
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                if (fr != null) {
                    fr!!.close()
                }
                if (br != null) {
                    br!!.close()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return show_seq
    }

    fun writeToFile(show_seq: Int) {
        //获取当前日期字符串
        val sdf = SimpleDateFormat("yyyy-MM-dd")
        val data = sdf.format(Date())
        val content = "$data,$show_seq"
        //读取本地缓存show_seq的文件
        try {
            val rootPath = Environment.getExternalStorageDirectory().path
            var file = File("$rootPath/Android/data/com.snssdk.api/cache/")
            if (!file.exists()) {
                file.mkdir()
            }
            val filename = file.absolutePath + "/adloadSeqTemp.txt"
            file = File(filename)
            if (!file.exists()) {
                file.createNewFile()
            }
            fw = FileWriter(file, false)
            fw!!.write(content)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                if (fw != null) {
                    fw!!.flush()
                    fw!!.close()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}