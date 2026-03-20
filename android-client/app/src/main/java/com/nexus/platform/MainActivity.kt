package com.nexus.platform

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nexus.platform.adapter.GameListAdapter
import com.nexus.platform.model.Game

/**
 * 主Activity，显示游戏列表
 */
class MainActivity : AppCompatActivity() {

    private lateinit var gameListRecyclerView: RecyclerView
    private lateinit var gameListAdapter: GameListAdapter
    private val games = mutableListOf<Game>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        loadGames()
    }

    /**
     * 初始化视图组件
     */
    private fun initViews() {
        gameListRecyclerView = findViewById(R.id.gameListRecyclerView)
        gameListRecyclerView.layoutManager = LinearLayoutManager(this)
        gameListAdapter = GameListAdapter(games) { game ->
            GameActivity.start(this, game)
        }
        gameListRecyclerView.adapter = gameListAdapter
    }

    /**
     * 加载游戏列表数据
     */
    private fun loadGames() {
        games.clear()
        games.add(Game(
            id = "1",
            name = "示例游戏",
            description = "这是一个示例游戏",
            iconUrl = "",
            downloadUrl = "https://example.com/game1.zip",
            version = "1.0.0"
        ))
        gameListAdapter.notifyDataSetChanged()
    }
}
