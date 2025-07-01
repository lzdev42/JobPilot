package jobpanel

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import components.input.ChipInput
import components.dropdown.BossConfigDropdown
import logview.LogView
import utils.AppConfig

@Composable
@Preview
fun JobPanelView(model: JobPanelViewModel) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = { model.startJobSeeker() }) {
                Text(if (model.isRunning) "停止" else "开始")
            }

            ChipInput(
                value = model.keywords,
                onValueChange = { model.updateKeywords(it) },
                modifier = Modifier.weight(1f),
                placeholder = "输入关键词，用逗号或空格分隔"
            )
        }

        // Boss 配置选项，仅在 BOSS 类型时显示
        if (model.seekerType == SeekerType.BOSS) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BossConfigDropdown(
                    label = "工作类型",
                    items = AppConfig.getBossConfig()?.jobType ?: emptyMap(),
                    selectedValue = model.bossFilter.jobType,
                    onValueChange = { model.bossFilter = model.bossFilter.copy(jobType = it) },
                    modifier = Modifier.weight(1f)
                )

                BossConfigDropdown(
                    label = "经验要求",
                    items = AppConfig.getBossConfig()?.experience ?: emptyMap(),
                    selectedValue = model.bossFilter.experience,
                    onValueChange = { model.bossFilter = model.bossFilter.copy(experience = it) },
                    modifier = Modifier.weight(1f)
                )

                BossConfigDropdown(
                    label = "学历要求",
                    items = AppConfig.getBossConfig()?.degree ?: emptyMap(),
                    selectedValue = model.bossFilter.degree,
                    onValueChange = { model.bossFilter = model.bossFilter.copy(degree = it) },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BossConfigDropdown(
                    label = "公司规模",
                    items = AppConfig.getBossConfig()?.scale ?: emptyMap(),
                    selectedValue = model.bossFilter.scale,
                    onValueChange = { model.bossFilter = model.bossFilter.copy(scale = it) },
                    modifier = Modifier.weight(1f)
                )

                BossConfigDropdown(
                    label = "城市",
                    items = AppConfig.getBossConfig()?.city ?: emptyMap(),
                    selectedValue = model.bossFilter.city,
                    onValueChange = { model.bossFilter = model.bossFilter.copy(city = it) },
                    modifier = Modifier.weight(1f)
                )

                BossConfigDropdown(
                    label = "薪资范围",
                    items = AppConfig.getBossConfig()?.salary ?: emptyMap(),
                    selectedValue = model.bossFilter.salary,
                    onValueChange = { model.bossFilter = model.bossFilter.copy(salary = it) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        LogView(model.logViewModel, modifier = Modifier.fillMaxSize().padding(10.dp))
    }
}