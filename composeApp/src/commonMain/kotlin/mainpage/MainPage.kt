package mainpage

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.TabRow
import androidx.compose.material.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.material.Text
import androidx.compose.ui.graphics.Color
import inputview.InputType
import inputview.InputView
import inputview.InputViewModel
import jobpanel.JobPanelView
import jobpanel.JobPanelViewModel
import jobpanel.SeekerType
import setting.SettingView

@Composable
@Preview
fun MainPage(modifier: Modifier) {
    val tabItems = listOf<String>("boss","51job","猎聘","个人简介","Prompts","设置")
    var selectedTabIndex by remember { mutableStateOf(0) }
    val bossViewModel = remember { JobPanelViewModel(SeekerType.BOSS) }
    val job51ViewModel = remember { JobPanelViewModel(SeekerType.JOB51) }
    val liepinViewModel = remember { JobPanelViewModel(SeekerType.LIEPIN) }
    val resumeViewModel = remember { InputViewModel(InputType.RESUME) }
    val promptViewModel = remember { InputViewModel(InputType.PROMPT) }
    val settingViewModel = remember { InputViewModel(InputType.SETTING) }
    Column(modifier = modifier) {
        TabRow(
            selectedTabIndex = selectedTabIndex,
            backgroundColor = Color.Transparent, // TabRow 背景色
            contentColor = MaterialTheme.colors.primary,      // 选中 Tab 内容颜色
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    //PrimaryNavigationTabTokens.ActiveIndicatorHeight, color = MaterialTheme.colors.primary // 指示器颜

                )
            }
        ) {
            tabItems.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title) },
                    selectedContentColor = MaterialTheme.colors.primary,
                    unselectedContentColor = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium)
                )
            }

        }

        // 根据选中的 Tab 动态显示对应的页面内容
        //Spacer(Modifier.height(8.dp)) // 可选：在 TabRow 和内容之间添加一些间距

        // 使用 Box 来承载可切换的内容，并应用 weight 使其填充剩余空间
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTabIndex) {
                0 -> JobPanelView(bossViewModel)
                1 -> JobPanelView(job51ViewModel)
                2 -> JobPanelView(liepinViewModel)
                3 -> InputView(resumeViewModel,Modifier.fillMaxSize())
                4 -> InputView(promptViewModel,Modifier.fillMaxSize())
                5 -> SettingView(settingViewModel,Modifier.fillMaxSize())
            }
        }
    }
}