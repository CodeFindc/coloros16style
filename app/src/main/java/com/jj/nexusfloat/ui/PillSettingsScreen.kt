package com.jj.nexusfloat.ui

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.jj.nexusfloat.ui.theme.*
import com.mess1on.combinedstatus.BatteryStyle
import com.mess1on.combinedstatus.BuildInfo
import com.mess1on.combinedstatus.ColorOsPill
import com.mess1on.combinedstatus.PillPreviewView
import com.mess1on.combinedstatus.ScopeRestarter
import com.mess1on.combinedstatus.StatusColorPicker
import com.mess1on.combinedstatus.StatusColors
import com.mess1on.combinedstatus.StyleProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val STYLE_KEYS = arrayOf(
    "merge", "size", "weight", "threshold", "show_percent", "colour",
    "batt_scale", "batt_weight", "centre_scale", "centre_weight",
    "wifi_scale", "wifi_weight", "dots_scale", "dots_weight",
    "power_color", "color_charging", "color_low", "color_performance",
    "swap_mid", "look_preset", "global_scale", "icon_gap",
    "pct_dx", "pct_dy", "pct_track", "exit_anim",
    "burn_in", "burn_sec", "burn_px", "burn_test",
    "bolt_scale", "bolt_dx", "bolt_dy", "bolt_double",
    "wifi_over_sim", "color_normal", "bolt_show", "color_alpha",
    "alpha_follow", "icon_trim", "icon_auto", "color_normal_light",
    "wifi_dx", "wifi_dy", "tech_dx", "tech_dy",
    "pill_dx", "pill_dy", "pill_dx_l", "pill_dy_l",
    "weight_link", "bolt_center", "color_fast", "color_super",
    "color_wireless", "dots_rainbow", "dot_c1", "dot_c2",
    "dot_c3", "dot_c4", "qs_takeover", "dots_auto",
    "dots_dual", "dots_line", "dots_dy", "dot_c5",
    "dot_c6", "dot_c7", "dot_c8", "show_5ga",
    "plane_scale", "centre_5ga_scale", "room", "tech_grey",
    "gap_extra", "capsule_dual", "charge_digit", "capsule_gap",
    "batt_sys_hide", "capsule_time", "hide_clock"
)

private val BOOL_PREF_KEYS = setOf(
    "merge", "show_percent", "colour", "swap_mid",
    "exit_anim", "burn_in", "wifi_over_sim", "preview_dark"
)

private fun defaultForKey(k: String): Int = when (k) {
    "merge", "show_percent", "colour", "exit_anim", "burn_in",
    "bolt_show", "alpha_follow", "icon_auto", "qs_takeover",
    "dots_auto", "tech_grey", "charge_digit" -> 1
    "size" -> 24
    "threshold" -> 20
    "burn_sec" -> 60
    "burn_px" -> 1
    "bolt_scale" -> 155
    "bolt_dx" -> 95
    "bolt_dy" -> 88
    "weight", "batt_scale", "batt_weight", "centre_scale", "centre_weight",
    "wifi_scale", "wifi_weight", "dots_scale", "dots_weight",
    "global_scale", "color_alpha", "icon_trim", "plane_scale" -> 100
    else -> 0
}

@Composable
fun PillSettingsScreen() {
    val context = LocalContext.current
    val activity = context as? Activity
    val sp = remember { StyleProvider.prefs(context) }

    // 用来驱动实时预览 Canvas 与全页面响应式刷新
    var revision by remember { mutableIntStateOf(0) }
    var scenario by remember { mutableIntStateOf(0) }

    fun readInt(key: String, def: Int = defaultForKey(key)): Int {
        @Suppress("UNUSED_EXPRESSION")
        revision
        return try {
            sp.getInt(key, def)
        } catch (_: ClassCastException) {
            try {
                if (sp.getBoolean(key, def != 0)) 1 else 0
            } catch (_: Throwable) {
                def
            }
        }
    }

    fun readBool(key: String, def: Boolean = defaultForKey(key) != 0): Boolean {
        return readInt(key, if (def) 1 else 0) != 0
    }

    fun broadcastAll(diag: Boolean = false) {
        try {
            context.contentResolver.notifyChange(StyleProvider.URI, null)
        } catch (_: Throwable) {
        }
        try {
            val intent = Intent(ColorOsPill.ACTION_STYLE)
            for (k in STYLE_KEYS) {
                intent.putExtra(k, readInt(k, defaultForKey(k)))
            }
            if (diag) {
                intent.putExtra("diag", true)
            }
            context.sendBroadcast(intent)
        } catch (_: Throwable) {
        }
    }

    fun writeVal(key: String, value: Int, notify: Boolean = true) {
        val ed = sp.edit()
        if (key in BOOL_PREF_KEYS) {
            ed.putBoolean(key, value != 0)
        } else {
            ed.putInt(key, value)
        }
        if (key == "weight" && readBool("weight_link", false)) {
            ed.putInt("dots_weight", value)
        }
        ed.commit()
        revision++
        if (notify) {
            broadcastAll(false)
        }
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
                var count = 0
                text.lineSequence().forEach { line ->
                    val t = line.trim()
                    if (t.isNotEmpty() && !t.startsWith("#") && !t.startsWith("//") && '=' in t) {
                        val k = t.substringBefore('=').trim()
                        val v = t.substringAfter('=').trim()
                        if (k in STYLE_KEYS) {
                            v.toIntOrNull()?.let {
                                writeVal(k, it, notify = false)
                                count++
                            }
                        }
                    }
                }
                broadcastAll(false)
                Toast.makeText(context, "已从文件导入 $count 项参数", Toast.LENGTH_SHORT).show()
            } catch (t: Throwable) {
                Toast.makeText(context, "导入失败：${t.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. 接管总开关与实时预览卡片（含 8 个场景标签切换 + 点击切换深浅背景）
        PillCard(title = "接管与实时预览") {
            PillSwitchItem(
                title = "隐藏原生图标，胶囊接管",
                subtitle = "开启后自动接管 ColorOS 状态栏与锁屏的电池、Wi-Fi、移动信号与飞行模式图标",
                checked = readBool("merge", true),
                onCheckedChange = { writeVal("merge", if (it) 1 else 0) }
            )

            // 1:1 原版 Canvas 预览（左侧高清放大，右侧状态栏 1:1 真实尺寸，点击切换深浅底色）
            AndroidView(
                factory = { ctx ->
                    PillPreviewView(ctx).apply {
                        setOnClickListener {
                            val curDark = readBool("preview_dark", true)
                            writeVal("preview_dark", if (curDark) 0 else 1, notify = false)
                            invalidate()
                        }
                    }
                },
                update = { view ->
                    @Suppress("UNUSED_EXPRESSION")
                    revision
                    view.scenario = scenario
                    view.invalidate()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(128.dp)
                    .padding(vertical = 4.dp)
            )

            // 8 个场景切换芯片（正常 / 充电中 / 低电量 / 省电 / 飞行 / WiFi关 / WiFi未连 / 性能模式）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PillPreviewView.SCENES.forEachIndexed { idx, name ->
                    val selected = scenario == idx
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) AccentBlue.copy(alpha = 0.22f) else CardStroke.copy(alpha = 0.25f))
                            .border(
                                1.dp,
                                if (selected) AccentBlue else CardStroke.copy(alpha = 0.5f),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { scenario = idx }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = name,
                            color = if (selected) AccentBlue else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        // 2. 选择样式（圆环 vs 双卡胶囊，带实时缩略图磁贴）
        PillCard(title = "选择样式") {
            Text(
                text = "挑一种电量长相，点哪块用哪块。胶囊把两张卡的信号点、制式和数字画进同一枚里。",
                color = TextSecondary,
                fontSize = 12.sp
            )
            val isCapsule = readBool("capsule_dual", false)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StyleTileItem(
                    modifier = Modifier.weight(1f),
                    title = "圆环",
                    subtitle = "环 + 一排信号点",
                    capsule = false,
                    selected = !isCapsule,
                    revision = revision,
                    onClick = { writeVal("capsule_dual", 0) }
                )
                StyleTileItem(
                    modifier = Modifier.weight(1f),
                    title = "胶囊",
                    subtitle = "两卡并进同一枚",
                    capsule = true,
                    selected = isCapsule,
                    revision = revision,
                    onClick = { writeVal("capsule_dual", 1) }
                )
            }
        }

        // 3. 大小与形状（整颗胶囊 + 各块大小 · 粗细）
        PillFoldCard(title = "大小与形状", initiallyExpanded = true) {
            Text("整颗胶囊", color = AccentBlue, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
            PillEditableSliderItem(
                label = "图标尺寸",
                value = readInt("size", 24),
                range = 10..30,
                unit = "dp",
                onValueChange = { writeVal("size", it) }
            )
            PillEditableSliderItem(
                label = "整体缩放（乘在尺寸上）",
                value = readInt("global_scale", 100),
                range = 50..200,
                unit = "%",
                onValueChange = { writeVal("global_scale", it) }
            )
            PillEditableSliderItem(
                label = "描边粗细",
                value = readInt("weight", 100),
                range = 80..180,
                unit = "%",
                onValueChange = { writeVal("weight", it) }
            )
            PillEditableSliderItem(
                label = "圆环顶部开口 · 额外加宽",
                value = readInt("gap_extra", 0),
                range = 0..20,
                unit = "°",
                onValueChange = { writeVal("gap_extra", it) }
            )
            PillEditableSliderItem(
                label = "胶囊 · 电量数字与 5G 拉开距离",
                value = readInt("capsule_gap", 0),
                range = 0..100,
                unit = "%",
                onValueChange = { writeVal("capsule_gap", it) }
            )

            HorizontalDivider(color = CardStroke.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 4.dp))
            Text("各块大小 · 粗细（改完当帧生效）", color = AccentBlue, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)

            PillEditableSliderItem(
                label = "电量百分比 · 大小",
                value = readInt("batt_scale", 100),
                range = 50..200,
                unit = "%",
                onValueChange = { writeVal("batt_scale", it) }
            )
            PillEditableSliderItem(
                label = "电量数字 · 粗细（字重）",
                value = readInt("batt_weight", 100),
                range = 60..180,
                unit = "%",
                onValueChange = { writeVal("batt_weight", it) }
            )
            PillEditableSliderItem(
                label = "中间5G · 大小",
                value = readInt("centre_scale", 100),
                range = 50..200,
                unit = "%",
                onValueChange = { writeVal("centre_scale", it) }
            )
            PillEditableSliderItem(
                label = "中间5G · 粗细",
                value = readInt("centre_weight", 100),
                range = 30..250,
                unit = "%",
                onValueChange = { writeVal("centre_weight", it) }
            )
            PillEditableSliderItem(
                label = "中间WiFi · 大小",
                value = readInt("wifi_scale", 100),
                range = 50..200,
                unit = "%",
                onValueChange = { writeVal("wifi_scale", it) }
            )
            PillEditableSliderItem(
                label = "中间WiFi · 粗细",
                value = readInt("wifi_weight", 100),
                range = 30..250,
                unit = "%",
                onValueChange = { writeVal("wifi_weight", it) }
            )
            PillEditableSliderItem(
                label = "飞行模式 · 大小",
                value = readInt("plane_scale", 100),
                range = 50..200,
                unit = "%",
                onValueChange = { writeVal("plane_scale", it) }
            )
            PillEditableSliderItem(
                label = "中间5GA · 大小（0=跟5G）",
                value = readInt("centre_5ga_scale", 0),
                range = 0..200,
                unit = "%",
                onValueChange = { writeVal("centre_5ga_scale", it) }
            )
            Text(
                text = "只在开了「5G-Advanced 显示成 5GA」时有用。留 0 是跟 5G 一起缩放；拖了之后 5GA 按自己的比例走。",
                color = TextSecondary,
                fontSize = 11.5.sp
            )
            PillEditableSliderItem(
                label = "信号点 · 大小",
                value = readInt("dots_scale", 100),
                range = 50..200,
                unit = "%",
                onValueChange = { writeVal("dots_scale", it) }
            )
            PillSwitchItem(
                title = "圆环与信号点 · 粗细联动",
                checked = readBool("weight_link", false),
                onCheckedChange = {
                    writeVal("weight_link", if (it) 1 else 0)
                    if (it) {
                        writeVal("dots_weight", readInt("weight", 100))
                    }
                }
            )
            PillEditableSliderItem(
                label = "信号点 · 粗细",
                value = readInt("dots_weight", 100),
                range = 30..250,
                unit = "%",
                onValueChange = { writeVal("dots_weight", it) }
            )
            PillEditableSliderItem(
                label = "信号点 · 上下",
                value = readInt("dots_dy", 0),
                range = -24..24,
                unit = "px",
                onValueChange = { writeVal("dots_dy", it) }
            )
        }

        // 4. 电量与百分比（含充电色两档）
        PillFoldCard(title = "电量与百分比") {
            PillSwitchItem(
                title = "显示电量百分比",
                checked = readBool("show_percent", true),
                onCheckedChange = { writeVal("show_percent", if (it) 1 else 0) }
            )
            PillEditableSliderItem(
                label = "低电量变红阈值",
                value = readInt("threshold", 20),
                range = 1..100,
                unit = "%",
                onValueChange = { writeVal("threshold", it) }
            )
            PillSwitchItem(
                title = "彩色（充电绿 / 低电红 / 省电黄）",
                checked = readBool("colour", true),
                onCheckedChange = { writeVal("colour", if (it) 1 else 0) }
            )
            HorizontalDivider(color = CardStroke.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 4.dp))
            Text("充电色（两档）", color = AccentBlue, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
            PillColorPickerRow(
                activity = activity,
                label = "普通充电",
                key = "color_charging",
                currentColor = readInt("color_charging", 0),
                fallbackColor = 0xFF1CA053.toInt(),
                allowSystemDefault = false,
                onPick = { writeVal("color_charging", it) }
            )
            PillColorPickerRow(
                activity = activity,
                label = "快充",
                key = "color_fast",
                currentColor = readInt("color_fast", 0),
                fallbackColor = 0xFFFFD400.toInt(),
                allowSystemDefault = false,
                onPick = { writeVal("color_fast", it) }
            )
            PillSwitchItem(
                title = "充电时电量数字跟着变色",
                subtitle = "ColorOS 不把充电协议告诉状态栏，实测只报得出电流电压，所以只分两档：到快充线走「快充」，其余「普通充电」。充满按没充的颜色。",
                checked = readBool("charge_digit", true),
                onCheckedChange = { writeVal("charge_digit", if (it) 1 else 0) }
            )
        }

        // 5. 信号点与制式
        PillFoldCard(title = "信号点与制式") {
            PillSwitchItem(
                title = "智能检测双卡（几张卡画几行）",
                checked = readBool("dots_auto", true),
                onCheckedChange = { writeVal("dots_auto", if (it) 1 else 0) }
            )
            PillSwitchItem(
                title = "双卡两行信号点（上＝主卡，下＝另一张卡）",
                checked = readBool("dots_dual", false),
                onCheckedChange = { writeVal("dots_dual", if (it) 1 else 0) }
            )
            PillSwitchItem(
                title = "信号点横着排（默认跟圆环弯）",
                subtitle = "智能检测开着就按在用的卡数自动画一行或两行，下面那个手动开关不参与。上行是默认数据卡，下行是另一张。这两条只管圆环；选了双卡胶囊时不用动它们。",
                checked = readBool("dots_line", false),
                onCheckedChange = { writeVal("dots_line", if (it) 1 else 0) }
            )
            PillSwitchItem(
                title = "中间图标 ↔ 电量百分比 互换位置",
                checked = readBool("swap_mid", false),
                onCheckedChange = { writeVal("swap_mid", if (it) 1 else 0) }
            )
            PillSwitchItem(
                title = "胶囊里那一格显示时间",
                subtitle = "那串数字换成时间（跟系统 12/24 小时制）。电量还在：外框那一圈就是电量。",
                checked = readBool("capsule_time", false),
                onCheckedChange = { writeVal("capsule_time", if (it) 1 else 0) }
            )
            PillSwitchItem(
                title = "藏掉状态栏的原生时间",
                subtitle = "时间已经在胶囊里了，左上角那颗就不再显示；任一档关掉立刻回来。只运行期生效，不写系统设置。",
                checked = readBool("hide_clock", false),
                onCheckedChange = { writeVal("hide_clock", if (it) 1 else 0) }
            )
            PillSwitchItem(
                title = "优先显示 WiFi（用流量时也显示）",
                subtitle = "WiFi 开着还没连上时，中间画暗一档的 WiFi 弧线，不再提示无卡、也不显示制式字样。没有卡槽的机器建议开着。",
                checked = readBool("wifi_over_sim", false),
                onCheckedChange = { writeVal("wifi_over_sim", if (it) 1 else 0) }
            )
            PillSwitchItem(
                title = "5G-Advanced 显示成 5GA",
                subtitle = "5G-Advanced 网络上写 5GA，判断用系统自己的字段，读不到就仍写 5G。默认关，因为两格变三格会占宽。",
                checked = readBool("show_5ga", false),
                onCheckedChange = { writeVal("show_5ga", if (it) 1 else 0) }
            )
            PillSwitchItem(
                title = "关闭数据网时制式变灰（不隐藏）",
                subtitle = "流量总闸关掉、卡还注册着网络时，制式仍然显示，只是整颗淡一档灰。默认开。",
                checked = readBool("tech_grey", true),
                onCheckedChange = { writeVal("tech_grey", if (it) 1 else 0) }
            )
        }

        // 6. 颜色（状态色 + 省电快捷色 + 8 信号点分色 + 浓淡与透明度）
        PillFoldCard(title = "颜色") {
            Text("状态色（点一下改）", color = AccentBlue, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
            PillColorPickerRow(
                activity = activity,
                label = "低电色",
                key = "color_low",
                currentColor = readInt("color_low", 0),
                fallbackColor = 0xFFE73C3A.toInt(),
                allowSystemDefault = false,
                onPick = { writeVal("color_low", it) }
            )
            PillColorPickerRow(
                activity = activity,
                label = "省电模式色",
                key = "power_color",
                currentColor = readInt("power_color", 0),
                fallbackColor = 0xFFFFB300.toInt(),
                allowSystemDefault = false,
                onPick = { writeVal("power_color", it) }
            )
            // 省电模式 6 个快捷色按钮（黄 / 橙 / 红 / 绿 / 蓝 / 白）
            val quickSaverColors = listOf(
                "黄" to 0xFFFFCC00.toInt(),
                "橙" to 0xFFFF9500.toInt(),
                "红" to 0xFFFF3B30.toInt(),
                "绿" to 0xFF34C759.toInt(),
                "蓝" to 0xFF007AFF.toInt(),
                "白" to 0xFFFFFFFF.toInt()
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                quickSaverColors.forEach { (name, col) ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(col))
                            .border(1.dp, CardStroke, RoundedCornerShape(10.dp))
                            .clickable {
                                writeVal("power_color", col)
                                Toast.makeText(context, "省电颜色：$name", Toast.LENGTH_SHORT).show()
                            }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = name,
                            color = if (name == "白" || name == "黄") Color(0xFF1E293B) else Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Text(
                text = "点上面的色块开取色器；这一排是省电模式色的常用快捷色。",
                color = TextSecondary,
                fontSize = 11.5.sp
            )

            PillColorPickerRow(
                activity = activity,
                label = "性能模式色",
                key = "color_performance",
                currentColor = readInt("color_performance", 0),
                fallbackColor = 0xFF2F6BED.toInt(),
                allowSystemDefault = false,
                onPick = { writeVal("color_performance", it) }
            )
            PillColorPickerRow(
                activity = activity,
                label = "正常色 · 深色背景下",
                key = "color_normal",
                currentColor = readInt("color_normal", 0),
                fallbackColor = 0xFFFFFFFF.toInt(),
                allowSystemDefault = true,
                onPick = { writeVal("color_normal", it) }
            )
            PillColorPickerRow(
                activity = activity,
                label = "正常色 · 浅色背景下",
                key = "color_normal_light",
                currentColor = readInt("color_normal_light", 0),
                fallbackColor = 0xFF141923.toInt(),
                allowSystemDefault = true,
                onPick = { writeVal("color_normal_light", it) }
            )
            Text(
                text = "两格都留空（跟随系统）＝完全照抄系统染色；只填一个＝那个背景用你定的色。系统判背景明暗时会自动在两色之间挑。",
                color = TextSecondary,
                fontSize = 11.5.sp
            )

            HorizontalDivider(color = CardStroke.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 4.dp))
            Text("信号点分色（8 点独立染色）", color = AccentBlue, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)

            PillColorPickerRow(activity, "信号点 1（最左）", "dot_c1", readInt("dot_c1", 0), 0xFF0A84FF.toInt(), false) { writeVal("dot_c1", it) }
            PillColorPickerRow(activity, "信号点 2", "dot_c2", readInt("dot_c2", 0), 0xFF5E5CE6.toInt(), false) { writeVal("dot_c2", it) }
            PillColorPickerRow(activity, "信号点 3", "dot_c3", readInt("dot_c3", 0), 0xFFBF5AF2.toInt(), false) { writeVal("dot_c3", it) }
            PillColorPickerRow(activity, "信号点 4（最右）", "dot_c4", readInt("dot_c4", 0), 0xFFFF375F.toInt(), false) { writeVal("dot_c4", it) }

            PillSwitchItem(
                title = "信号点分色（用上面四个颜色）",
                subtitle = "打开后四个点各用自己那一格的颜色，没连上的点仍按原样压暗。默认蓝→靛→紫→粉。",
                checked = readBool("dots_rainbow", false),
                onCheckedChange = { writeVal("dots_rainbow", if (it) 1 else 0) }
            )

            PillColorPickerRow(activity, "卡2 点 1（最左）", "dot_c5", readInt("dot_c5", 0), 0xFF64D2FF.toInt(), false) { writeVal("dot_c5", it) }
            PillColorPickerRow(activity, "卡2 点 2", "dot_c6", readInt("dot_c6", 0), 0xFF30D158.toInt(), false) { writeVal("dot_c6", it) }
            PillColorPickerRow(activity, "卡2 点 3", "dot_c7", readInt("dot_c7", 0), 0xFFFFD60A.toInt(), false) { writeVal("dot_c7", it) }
            PillColorPickerRow(activity, "卡2 点 4（最右）", "dot_c8", readInt("dot_c8", 0), 0xFFFF9F0A.toInt(), false) { writeVal("dot_c8", it) }
            Text(
                text = "下面四个只在「双卡两行信号点」开着时画第二行，也要先打开上面的「信号点分色」。\n优先级：省电 > 性能 > 充电 > 低电 > 常规；关掉「彩色」就全跟随系统染色。",
                color = TextSecondary,
                fontSize = 11.5.sp
            )

            HorizontalDivider(color = CardStroke.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 4.dp))
            Text("浓淡与透明度", color = AccentBlue, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)

            PillSwitchItem(
                title = "不透明度跟随系统图标",
                checked = readBool("alpha_follow", true),
                onCheckedChange = { writeVal("alpha_follow", if (it) 1 else 0) }
            )
            val rawAlpha = readInt("color_alpha", 100)
            val normAlpha = if (rawAlpha > 100) rawAlpha / 100 else rawAlpha
            PillEditableSliderItem(
                label = "图标透明度 %（关掉上面才生效）",
                value = normAlpha.coerceIn(10, 100),
                range = 10..100,
                unit = "%",
                onValueChange = { writeVal("color_alpha", it) }
            )
            PillSwitchItem(
                title = "自动跟随旁边图标的浓淡",
                checked = readBool("icon_auto", true),
                onCheckedChange = { writeVal("icon_auto", if (it) 1 else 0) }
            )
            val rawTrim = readInt("icon_trim", 100)
            val normTrim = if (rawTrim > 100) rawTrim / 100 else rawTrim
            PillEditableSliderItem(
                label = "与邻图标对齐 %（关掉上面才生效）",
                value = normTrim.coerceIn(40, 100),
                range = 40..100,
                unit = "%",
                onValueChange = { writeVal("icon_trim", it) }
            )
        }

        // 7. 充电闪电
        PillFoldCard(title = "充电闪电") {
            PillEditableSliderItem(
                label = "闪电大小",
                value = readInt("bolt_scale", 155),
                range = 30..220,
                unit = "%",
                onValueChange = { writeVal("bolt_scale", it) }
            )
            PillSegmentedRow(
                label = "闪充叠加",
                options = listOf("自动", "强制叠", "不叠"),
                selectedIndex = readInt("bolt_double", 0).coerceIn(0, 2),
                onSelect = { writeVal("bolt_double", it) }
            )
            PillSwitchItem(
                title = "充电时显示小闪电",
                checked = readBool("bolt_show", true),
                onCheckedChange = { writeVal("bolt_show", if (it) 1 else 0) }
            )
            PillSwitchItem(
                title = "充电时闪电进圆环中心",
                subtitle = "位置按电量环的宽高等比例走，换图标尺寸不用重调。默认贴右下角；检测到原装快充会上下叠成两颗。",
                checked = readBool("bolt_center", false),
                onCheckedChange = { writeVal("bolt_center", if (it) 1 else 0) }
            )
        }

        // 8. 位置微调（px / 百分比，改完当帧生效）
        PillFoldCard(title = "位置微调") {
            Text(
                text = "px / 百分比，改完当帧生效（点右侧数值可直接输入精确数字）",
                color = AccentBlue,
                fontSize = 12.sp
            )
            PillEditableSliderItem("整体位置 · 左右", readInt("pill_dx", 0), -1440..1440, "px") { writeVal("pill_dx", it) }
            PillEditableSliderItem("整体位置 · 上下", readInt("pill_dy", 0), -400..400, "px") { writeVal("pill_dy", it) }
            PillEditableSliderItem("横屏位置 · 左右", readInt("pill_dx_l", 0), -1440..1440, "px") { writeVal("pill_dx_l", it) }
            PillEditableSliderItem("横屏位置 · 上下", readInt("pill_dy_l", 0), -400..400, "px") { writeVal("pill_dy_l", it) }
            PillEditableSliderItem("与隔壁图标的空隙", readInt("icon_gap", 0), -30..24, "dp") { writeVal("icon_gap", it) }
            PillEditableSliderItem("电量数字 · 左右", readInt("pct_dx", 0), -24..24, "px") { writeVal("pct_dx", it) }
            PillEditableSliderItem("电量数字 · 上下", readInt("pct_dy", 0), -24..24, "px") { writeVal("pct_dy", it) }
            PillEditableSliderItem("电量数字 · 字间距", readInt("pct_track", 0), -50..150, "%") { writeVal("pct_track", it) }
            PillEditableSliderItem("中间5G · 左右", readInt("tech_dx", 0), -24..24, "px") { writeVal("tech_dx", it) }
            PillEditableSliderItem("中间5G · 上下", readInt("tech_dy", 0), -24..24, "px") { writeVal("tech_dy", it) }
            PillEditableSliderItem("中间WiFi · 左右", readInt("wifi_dx", 0), -24..24, "px") { writeVal("wifi_dx", it) }
            PillEditableSliderItem("中间WiFi · 上下", readInt("wifi_dy", 0), -24..24, "px") { writeVal("wifi_dy", it) }
            PillEditableSliderItem("充电闪电 · 横向（50 居中，100 环右）", readInt("bolt_dx", 95), 0..170, "%") { writeVal("bolt_dx", it) }
            PillEditableSliderItem("充电闪电 · 纵向（50 居中，越大越往下）", readInt("bolt_dy", 88), 0..170, "%") { writeVal("bolt_dy", it) }
            Text(
                text = "横竖屏各记一份：转屏后压到别的图标或撞到摄像头，单独调横屏那两条。",
                color = TextSecondary,
                fontSize = 11.5.sp
            )
        }

        // 9. 控制中心 · 兼容 · 动效（含防烧屏）
        PillFoldCard(title = "控制中心 · 兼容 · 动效") {
            PillSwitchItem(
                title = "控制中心头部也换成胶囊",
                subtitle = "只管控制中心顶部那一行。关掉就交还原生；个别机型要再重启一次系统界面才完全复原。",
                checked = readBool("qs_takeover", true),
                onCheckedChange = { writeVal("qs_takeover", if (it) 1 else 0) }
            )
            PillSwitchItem(
                title = "强制兼容性",
                subtitle = "开着时原生电池不再被隐藏，但不画也不占位——给按「电池在不在」判断的别的模块让路。默认关。",
                checked = readBool("room", false),
                onCheckedChange = { writeVal("room", if (it) 1 else 0) }
            )
            PillSwitchItem(
                title = "电池关不住时用系统隐藏",
                subtitle = "个别机型按不住原生电池（胶囊旁边又冒一颗方块），这一档才借系统设置去藏。卸载前先关掉，或点下方「找回原生电池」。默认关。",
                checked = readBool("batt_sys_hide", false),
                onCheckedChange = { writeVal("batt_sys_hide", if (it) 1 else 0) }
            )
            PillSwitchItem(
                title = "下拉退场动画（实验性：点右到左消失 + 环扫一圈）",
                checked = readBool("exit_anim", true),
                onCheckedChange = { writeVal("exit_anim", if (it) 1 else 0) }
            )

            HorizontalDivider(color = CardStroke.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 4.dp))
            Text("防烧屏（微动）", color = AccentBlue, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)

            PillSwitchItem(
                title = "防烧屏微动（每隔一会儿把胶囊挪 1px，肉眼看不出来）",
                checked = readBool("burn_in", true),
                onCheckedChange = { writeVal("burn_in", if (it) 1 else 0) }
            )
            PillEditableSliderItem(
                label = "微动间隔",
                value = readInt("burn_sec", 60),
                range = 20..300,
                unit = "s",
                onValueChange = { writeVal("burn_sec", it) }
            )
            PillEditableSliderItem(
                label = "微动幅度",
                value = readInt("burn_px", 1),
                range = 1..3,
                unit = "px",
                onValueChange = { writeVal("burn_px", it) }
            )
            OutlinedButton(
                onClick = {
                    val next = readInt("burn_test", 0) + 1
                    writeVal("burn_test", next)
                    Toast.makeText(context, "已让胶囊挪一格：盯着状态栏看会抖一下；诊断里「手动」次数 +1", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("手动挪一格（验证防烧屏）", fontSize = 13.sp)
            }
            Text(
                text = "OLED 上静止的图标会烧出残影。开着就每分钟在 3×3 的格子里挪一格（九分钟一圈），不会挪出容器，下拉动画期间不挪。",
                color = TextSecondary,
                fontSize = 11.5.sp
            )
        }

        // 10. 备份与高级（导出 / 导入 / 找回原生电池 / 重启系统界面 / 实时诊断回报）
        PillFoldCard(title = "备份与高级") {
            Text(
                text = "把当前所有调过的参数写成纯文本（key=value）。导入时只认列出来的键，没出现的键保持本机现在的值。",
                color = TextSecondary,
                fontSize = 11.5.sp
            )
            fun buildStyleExportText(): String {
                val sb = StringBuilder()
                val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                sb.append("# 三合一 Status bar 参数\n# 版本 ").append(BuildInfo.NAME)
                    .append("（CODE ").append(BuildInfo.CODE).append("） · ").append(stamp).append("\n")
                for (k in STYLE_KEYS) {
                    sb.append(k).append('=').append(readInt(k, defaultForKey(k))).append('\n')
                }
                return sb.toString()
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        cm?.setPrimaryClip(ClipData.newPlainText("参数文本", buildStyleExportText()))
                        Toast.makeText(context, "已复制：参数文本", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("复制到剪贴板", fontSize = 12.5.sp)
                }
                OutlinedButton(
                    onClick = {
                        try {
                            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "三合一 Status bar")
                            dir.mkdirs()
                            val fn = "参数_${SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())}.txt"
                            val file = File(dir, fn)
                            file.writeText(buildStyleExportText(), Charsets.UTF_8)
                            Toast.makeText(context, "已保存：Download/三合一 Status bar/$fn", Toast.LENGTH_LONG).show()
                        } catch (t: Throwable) {
                            Toast.makeText(context, "写文件失败：${t.message}，请先用复制到剪贴板", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("导出为文件", fontSize = 12.5.sp)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { filePicker.launch("*/*") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("导入参数（选文件）", fontSize = 12.5.sp)
                }
                OutlinedButton(
                    onClick = {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        val text = cm?.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                        if (text.isBlank()) {
                            Toast.makeText(context, "剪贴板为空", Toast.LENGTH_SHORT).show()
                            return@OutlinedButton
                        }
                        var count = 0
                        text.lineSequence().forEach { line ->
                            val t = line.trim()
                            if (t.isNotEmpty() && !t.startsWith("#") && !t.startsWith("//") && '=' in t) {
                                val k = t.substringBefore('=').trim()
                                val v = t.substringAfter('=').trim()
                                if (k in STYLE_KEYS) {
                                    v.toIntOrNull()?.let {
                                        writeVal(k, it, notify = false)
                                        count++
                                    }
                                }
                            }
                        }
                        broadcastAll(false)
                        Toast.makeText(context, "已从剪贴板导入 $count 项参数", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("从剪贴板导入", fontSize = 12.5.sp)
                }
            }

            HorizontalDivider(color = CardStroke.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 4.dp))

            OutlinedButton(
                onClick = {
                    val msg = BatteryStyle.stripSlot(context)
                    Toast.makeText(context, "原生电池隐藏表处理结果：$msg", Toast.LENGTH_LONG).show()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("找回原生电池（擦掉系统 icon_blacklist 里的 battery）", fontSize = 12.5.sp)
            }

            Button(
                onClick = {
                    Toast.makeText(context, "正在重启系统界面…", Toast.LENGTH_SHORT).show()
                    Thread {
                        val ok = ScopeRestarter.run({ Runtime.getRuntime().exec(arrayOf("su", "-c")) }, 4000)
                        (context as? Activity)?.runOnUiThread {
                            Toast.makeText(
                                context,
                                if (ok) "已发送重启信号，状态栏会闪一下" else "重启失败，请确认已授予 Root 权限",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }.start()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("重启系统界面（重载模块）", fontSize = 13.sp)
            }

            HorizontalDivider(color = CardStroke.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 4.dp))

            var reportText by remember { mutableStateOf(sp.getString("report", "") ?: "") }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("SystemUI 胶囊实时诊断回报", color = TextPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(onClick = {
                        broadcastAll(diag = true)
                        reportText = sp.getString("report", "") ?: ""
                    }) {
                        Text("刷新", fontSize = 12.sp)
                    }
                    TextButton(onClick = {
                        val rep = sp.getString("report", "") ?: ""
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        cm?.setPrimaryClip(ClipData.newPlainText("三合一诊断", rep.ifEmpty { "尚未收到回报" }))
                        Toast.makeText(context, "诊断信息已复制", Toast.LENGTH_SHORT).show()
                    }) {
                        Text("复制", fontSize = 12.sp)
                    }
                }
            }
            Text(
                text = reportText.ifEmpty { "（尚未收到 SystemUI 回报，确认 LSPosed 已勾选系统界面并重启 SystemUI）" },
                color = TextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun PillCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(CardSurface)
            .border(1.dp, CardStroke, RoundedCornerShape(22.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        content()
    }
}

@Composable
private fun PillFoldCard(
    title: String,
    initiallyExpanded: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(CardSurface)
            .border(1.dp, CardStroke, RoundedCornerShape(22.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 15.5.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (expanded) "－" else "＋",
                color = TextSecondary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                content = content
            )
        }
    }
}

@Composable
private fun StyleTileItem(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    capsule: Boolean,
    selected: Boolean,
    revision: Int,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) AccentBlue.copy(alpha = 0.14f) else CardStroke.copy(alpha = 0.18f))
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) AccentBlue else CardStroke,
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        AndroidView(
            factory = { ctx -> PillPreviewView.createThumb(ctx, capsule) },
            update = { view ->
                @Suppress("UNUSED_EXPRESSION")
                revision
                view.invalidate()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        )
        Text(
            text = title,
            color = if (selected) AccentBlue else TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = subtitle,
            color = TextSecondary,
            fontSize = 11.5.sp
        )
    }
}

@Composable
private fun PillSwitchItem(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f).padding(end = 8.dp)
            )
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
        if (!subtitle.isNullOrEmpty()) {
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 11.5.sp
            )
        }
    }
}

@Composable
private fun PillEditableSliderItem(
    label: String,
    value: Int,
    range: IntRange,
    unit: String,
    onValueChange: (Int) -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var textInput by remember(value) { mutableStateOf(value.toString()) }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("直接输入数值（范围 ${range.first} ~ ${range.last}$unit）：", fontSize = 13.sp)
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    textInput.trim().toIntOrNull()?.let { v ->
                        onValueChange(v.coerceIn(range.first, range.last))
                    }
                    showEditDialog = false
                }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, color = TextPrimary, fontSize = 13.5.sp, modifier = Modifier.weight(1f))
            Text(
                text = "$value$unit ✎",
                color = AccentBlue,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable {
                        textInput = value.toString()
                        showEditDialog = true
                    }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
        Slider(
            value = value.coerceIn(range.first, range.last).toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = range.first.toFloat()..range.last.toFloat()
        )
    }
}

@Composable
private fun PillSegmentedRow(
    label: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(text = "$label：${options.getOrElse(selectedIndex) { "" }}", color = TextPrimary, fontSize = 13.5.sp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEachIndexed { idx, opt ->
                val active = idx == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (active) AccentBlue.copy(alpha = 0.22f) else CardStroke.copy(alpha = 0.2f))
                        .border(1.dp, if (active) AccentBlue else CardStroke, RoundedCornerShape(12.dp))
                        .clickable { onSelect(idx) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = opt,
                        color = if (active) AccentBlue else TextSecondary,
                        fontSize = 12.5.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun PillColorPickerRow(
    activity: Activity?,
    label: String,
    key: String,
    currentColor: Int,
    fallbackColor: Int,
    allowSystemDefault: Boolean,
    onPick: (Int) -> Unit
) {
    val resolved = StatusColors.resolve(currentColor, fallbackColor)
    val valueLabel = if (currentColor == 0) {
        if (allowSystemDefault) "跟随系统" else "默认 (${StatusColors.hex(fallbackColor)})"
    } else {
        StatusColors.hex(currentColor)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable {
                if (activity != null) {
                    StatusColorPicker.show(activity, label, currentColor, fallbackColor, allowSystemDefault) { picked ->
                        onPick(picked)
                    }
                }
            }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "$label（点一下改）", color = TextPrimary, fontSize = 13.5.sp)
            Text(text = valueLabel, color = TextSecondary, fontSize = 11.5.sp, fontFamily = FontFamily.Monospace)
        }
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Color(resolved))
                .border(1.dp, CardStroke, CircleShape)
        )
    }
}
