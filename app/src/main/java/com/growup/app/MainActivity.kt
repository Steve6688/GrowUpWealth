package com.growup.app

import android.os.Bundle
import android.app.DatePickerDialog
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.util.UUID

private val Green = Color(0xFF138B68)
private val GreenDark = Color(0xFF08634F)
private val Ink = Color(0xFF16231F)
private val Mint = Color(0xFFE4F3EC)
private val Red = Color(0xFFB3261E)
private val Canvas = Color(0xFFF9F6F0)
private val Glass = Color.White.copy(alpha = .74f)

enum class AccountKind(val title: String, val isLiability: Boolean) {
    CASH("现金", false), BANK("银行卡", false), WALLET("电子钱包", false), INVESTMENT("投资", false), LIABILITY("负债", true)
}
enum class TxKind(val title: String) { INCOME("收入"), EXPENSE("支出"), TRANSFER("转账") }
data class Account(val id: String = UUID.randomUUID().toString(), val name: String, val kind: AccountKind, val balance: Double, val updatedAt: LocalDate = LocalDate.now())
data class Transaction(val id: String = UUID.randomUUID().toString(), val title: String, val kind: TxKind, val amount: Double, val category: String, val date: LocalDate = LocalDate.now())
data class Goal(val name: String, val target: Double, val current: Double, val due: LocalDate)
data class Advice(val icon: ImageVector, val title: String, val reason: String, val action: String)

/** Future connectors may read balances and transactions only. OAuth credentials stay with the provider. */
interface AccountConnector { suspend fun readAccounts(): List<Account>; suspend fun readTransactions(): List<Transaction> }

class FinanceViewModel {
    var accounts by mutableStateOf(listOf(Account(name = "现金", kind = AccountKind.CASH, balance = 12340.0), Account(name = "招商银行", kind = AccountKind.BANK, balance = 86200.0), Account(name = "投资账户", kind = AccountKind.INVESTMENT, balance = 169990.0), Account(name = "信用卡负债", kind = AccountKind.LIABILITY, balance = 82000.0)))
        private set
    var transactions by mutableStateOf(listOf(
        Transaction(title = "工资收入", kind = TxKind.INCOME, amount = 12800.0, category = "工资", date = LocalDate.now().withDayOfMonth(5)),
        Transaction(title = "外卖", kind = TxKind.EXPENSE, amount = 28.0, category = "餐饮", date = LocalDate.now()),
        Transaction(title = "购物", kind = TxKind.EXPENSE, amount = 1498.0, category = "购物", date = LocalDate.now().minusDays(1)),
        Transaction(title = "地铁", kind = TxKind.EXPENSE, amount = 936.0, category = "交通", date = LocalDate.now().minusDays(2)),
        Transaction(title = "日用", kind = TxKind.EXPENSE, amount = 749.0, category = "日用", date = LocalDate.now().minusDays(3)),
        Transaction(title = "娱乐", kind = TxKind.EXPENSE, amount = 562.0, category = "娱乐", date = LocalDate.now().minusDays(4)),
        Transaction(title = "其他", kind = TxKind.EXPENSE, amount = 498.0, category = "其他", date = LocalDate.now().minusDays(5)),
        Transaction(title = "餐饮消费", kind = TxKind.EXPENSE, amount = 1969.0, category = "餐饮", date = LocalDate.now().minusDays(6))
    ))
        private set
    var goal by mutableStateOf(Goal("应急备用金", 50000.0, 30000.0, LocalDate.now().plusMonths(12)))
        private set

    val assets get() = accounts.filter { !it.kind.isLiability }.sumOf { it.balance }
    val liabilities get() = accounts.filter { it.kind.isLiability }.sumOf { it.balance }
    val netWorth get() = assets - liabilities
    private val monthTx get() = transactions.filter { YearMonth.from(it.date) == YearMonth.now() }
    val income get() = monthTx.filter { it.kind == TxKind.INCOME }.sumOf { it.amount }
    val expense get() = monthTx.filter { it.kind == TxKind.EXPENSE }.sumOf { it.amount }
    val surplus get() = income - expense
    val monthlyGoal: Double get() { val months = maxOf(1, ChronoUnit.MONTHS.between(YearMonth.now(), YearMonth.from(goal.due)).toInt()); return maxOf(0.0, (goal.target - goal.current) / months) }
    val advice get() = buildList {
        add(Advice(Icons.Default.BarChart, "控制餐饮支出", "你本月餐饮支出占比达到 32%，高于建议的 20%。", "预计每月可节省约 ¥400"))
        add(Advice(Icons.Default.Savings, "增加应急金储蓄", "你距离应急金目标还差 ¥20,000。", "建议设置每月自动存入 ¥2,000"))
        add(Advice(Icons.Default.AccountBalanceWallet, "完善账户数据", "已记录 3 个账户，持续同步后趋势会更准确。", "补记最近一笔消费，让建议更准确"))
    }.take(3)
    fun addAccount(name: String, kind: AccountKind, balance: Double) { accounts = accounts + Account(name = name, kind = kind, balance = balance) }
    fun addTransaction(title: String, kind: TxKind, amount: Double, category: String) { transactions = listOf(Transaction(title = title, kind = kind, amount = amount, category = category)) + transactions }
    fun updateGoal(name: String, target: Double, current: Double, due: LocalDate) { goal = Goal(name, target, current, due) }
}

class MainActivity : ComponentActivity() { override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { GrowUpApp() } } }

@Composable fun GrowUpApp() {
    val finance = remember { FinanceViewModel() }; var tab by rememberSaveable { mutableIntStateOf(0) }; var showAccount by remember { mutableStateOf(false) }; var showTx by remember { mutableStateOf(false) }; var showGoal by remember { mutableStateOf(false) }
    val labels = listOf("首页", "记账", "资产", "趋势", "建议"); val icons = listOf(Icons.Default.Home, Icons.Default.AddCircle, Icons.Default.AccountBalanceWallet, Icons.Default.ShowChart, Icons.Default.Lightbulb)
    MaterialTheme(colorScheme = lightColorScheme(primary = Green, secondary = Green, background = Canvas, surface = Color.White)) {
        Scaffold(containerColor = Canvas, bottomBar = { NavigationBar(containerColor = Canvas.copy(alpha = .94f), tonalElevation = 0.dp) { labels.forEachIndexed { i, label ->
            val selectedColor by animateColorAsState(if (tab == i) Green else Color.Gray, label = "navColor")
            NavigationBarItem(selected = tab == i, onClick = { tab = i }, colors = NavigationBarItemDefaults.colors(selectedIconColor = selectedColor, selectedTextColor = selectedColor, indicatorColor = Mint), icon = { Icon(icons[i], null) }, label = { Text(label) })
        } } }, floatingActionButton = { if (tab == 1) FloatingActionButton(onClick = { showTx = true }, containerColor = Green, contentColor = Color.White) { Icon(Icons.Default.Add, "新增记录") } }) { padding ->
            Box(Modifier.padding(padding)) { when (tab) { 0 -> Home(finance, { showTx = true }, { showAccount = true }, { showGoal = true }); 1 -> Ledger(finance); 2 -> Assets(finance, { showAccount = true }); 3 -> Trends(finance); else -> AdviceList(finance) } }
        }
        if (showAccount) AccountDialog({ showAccount = false }) { n, k, b -> finance.addAccount(n, k, b); showAccount = false }
        if (showTx) TransactionDialog({ showTx = false }) { n, k, a, c -> finance.addTransaction(n, k, a, c); showTx = false }
        if (showGoal) GoalDialog(finance.goal, { showGoal = false }) { n, t, c, d -> finance.updateGoal(n, t, c, d); showGoal = false }
    }
}

@Composable fun Home(f: FinanceViewModel, addTx: () -> Unit, addAccount: () -> Unit, editGoal: () -> Unit) = Page("") {
    var showNotice by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) { Text("◒", color = Green, fontSize = 25.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.width(7.dp)); Text("GrowUp", color = GreenDark, fontSize = 20.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.weight(1f)); Icon(Icons.Default.NotificationsNone, "通知", tint = Ink, modifier = Modifier.clickable { showNotice = true }) }
    Text("5月20日    星期二", color = Color.Gray, fontSize = 13.sp, modifier = Modifier.padding(top = 14.dp)); Text("今天也在认真生活 🌱", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.padding(top = 6.dp)); Spacer(Modifier.height(16.dp)); NetWorth(f); Spacer(Modifier.height(16.dp)); Section("本月现金流") { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Metric("收入", f.income, Green); Metric("支出", f.expense, Red); Metric("结余", f.surplus, Ink) } }; Spacer(Modifier.height(16.dp)); Section("今日建议", action = "查看全部") { f.advice.take(2).forEach { AdviceCard(it) } }; Spacer(Modifier.height(16.dp)); GoalCard(f.goal, f.monthlyGoal, editGoal); Spacer(Modifier.height(16.dp)); Text("最近记录", fontWeight = FontWeight.Bold); f.transactions.take(3).forEach { TransactionRow(it) }; Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) { OutlinedButton(onClick = addAccount, modifier = Modifier.weight(1f)) { Text("添加账户") }; Button(onClick = addTx, modifier = Modifier.weight(1f)) { Text("记一笔") } }
    if (showNotice) AlertDialog(onDismissRequest = { showNotice = false }, title = { Text("通知") }, text = { Text("暂无新的账务提醒\n\n你的账户数据已同步到今天。") }, confirmButton = { TextButton(onClick = { showNotice = false }) { Text("知道了") } })
}
@Composable fun Ledger(f: FinanceViewModel) = Page("记一笔") {
    var amount by rememberSaveable { mutableStateOf("68.00") }
    var category by rememberSaveable { mutableStateOf("餐饮") }
    var note by rememberSaveable { mutableStateOf("") }
    var accountName by rememberSaveable { mutableStateOf("招商银行（6231）") }
    var showAccounts by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().background(Color.White.copy(.72f), RoundedCornerShape(24.dp)).padding(4.dp), horizontalArrangement = Arrangement.SpaceEvenly) { Text("支出", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.background(Green, RoundedCornerShape(20.dp)).padding(horizontal = 52.dp, vertical = 11.dp)); Text("收入", color = Color.Gray, modifier = Modifier.padding(horizontal = 52.dp, vertical = 11.dp)) }
    Spacer(Modifier.height(18.dp)); OutlinedTextField(amount, { amount = it }, singleLine = true, textStyle = androidx.compose.ui.text.TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Bold), modifier = Modifier.fillMaxWidth(), label = { Text("金额") }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal)); Spacer(Modifier.height(16.dp)); Text("选择分类", fontWeight = FontWeight.Bold); Spacer(Modifier.height(10.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { LedgerCategory("餐饮", Icons.Default.Restaurant, category == "餐饮") { category = "餐饮" }; LedgerCategory("交通", Icons.Default.DirectionsTransit, category == "交通") { category = "交通" }; LedgerCategory("购物", Icons.Default.ShoppingBag, category == "购物") { category = "购物" }; LedgerCategory("日用", Icons.Default.ShoppingBasket, category == "日用") { category = "日用" } }
    Spacer(Modifier.height(14.dp)); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { LedgerCategory("娱乐", Icons.Default.SportsEsports, category == "娱乐") { category = "娱乐" }; LedgerCategory("医疗", Icons.Default.LocalHospital, category == "医疗") { category = "医疗" }; LedgerCategory("学习", Icons.Default.School, category == "学习") { category = "学习" }; LedgerCategory("其他", Icons.Default.MoreHoriz, category == "其他") { category = "其他" } }
    Spacer(Modifier.height(18.dp)); Section("选择账户", action = "切换", onAction = { showAccounts = true }) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.AccountBalance, null, tint = Green); Spacer(Modifier.width(10.dp)); Text(accountName, fontWeight = FontWeight.Medium); Spacer(Modifier.weight(1f)); Text("点击切换", color = Color.Gray) } }
    Spacer(Modifier.height(14.dp)); OutlinedTextField(note, { note = it }, label = { Text("添加备注") }, placeholder = { Text("例如：午餐、星巴克") }, modifier = Modifier.fillMaxWidth()); Spacer(Modifier.height(14.dp)); Button(onClick = { amount.toDoubleOrNull()?.let { f.addTransaction(note.ifBlank { category }, TxKind.EXPENSE, it, category) } }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) { Text("保存", fontSize = 17.sp) }
    if (showAccounts) AlertDialog(onDismissRequest = { showAccounts = false }, title = { Text("选择账户") }, text = { Column { f.accounts.forEach { account -> TextButton(onClick = { accountName = account.name; showAccounts = false }, modifier = Modifier.fillMaxWidth()) { Text("${account.name}  ¥${money(account.balance)}") } } } }, confirmButton = { TextButton(onClick = { showAccounts = false }) { Text("取消") } })
}
@Composable fun LedgerCategory(label: String, icon: ImageVector, selected: Boolean = false, onClick: () -> Unit = {}) { Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) { Surface(shape = RoundedCornerShape(18.dp), color = if (selected) Green else Color.White.copy(.72f), modifier = Modifier.size(52.dp)) { Icon(icon, null, tint = if (selected) Color.White else Color.Gray, modifier = Modifier.padding(14.dp)) }; Text(label, fontSize = 12.sp, modifier = Modifier.padding(top = 5.dp)) } }
@Composable fun Assets(f: FinanceViewModel, add: () -> Unit) = Page("资产") { NetWorth(f); Spacer(Modifier.height(18.dp)); Text("资产账户", fontWeight = FontWeight.Bold); f.accounts.filter { !it.kind.isLiability }.forEach { AccountRow(it) }; Spacer(Modifier.height(12.dp)); Text("负债", fontWeight = FontWeight.Bold); f.accounts.filter { it.kind.isLiability }.forEach { AccountRow(it) }; Spacer(Modifier.height(16.dp)); OutlinedButton(onClick = add, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("手动添加账户") }; Text("账户连接仅支持只读授权；不保存银行密码。", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 12.dp)) }
@Composable fun Trends(f: FinanceViewModel) = Page("") {
    var range by rememberSaveable { mutableStateOf("近3个月") }
    Row(verticalAlignment = Alignment.CenterVertically) { Text("◒", color = Green, fontSize = 23.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.width(6.dp)); Text("GrowUp", color = GreenDark, fontSize = 19.sp, fontWeight = FontWeight.Bold) }
    Text("趋势", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.padding(top = 12.dp, bottom = 10.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf("近7天", "近30天", "近3个月", "近1年").forEach { r -> TextButton(onClick = { range = r }, colors = ButtonDefaults.textButtonColors(containerColor = if (range == r) Green else Color.White.copy(.72f), contentColor = if (range == r) Color.White else Color.Gray), shape = RoundedCornerShape(18.dp), modifier = Modifier.weight(1f)) { Text(r, fontSize = 11.sp) } } }
    Spacer(Modifier.height(12.dp)); Section("净资产走势", action = range) { Text("¥${money(f.netWorth)}", fontSize = 21.sp, fontWeight = FontWeight.Bold); Text("▲ +16,220   +9.5%", color = Green, fontSize = 12.sp); ChartWithAxis { LineChart() }; Text("2月                 3月                 4月                 5月", color = Color.Gray, fontSize = 11.sp) }
    Spacer(Modifier.height(12.dp)); Section("收支趋势") { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { Text("● 收入", color = Green, fontSize = 11.sp); Spacer(Modifier.width(12.dp)); Text("● 支出", color = Color(0xFFE97865), fontSize = 11.sp) }; ChartWithAxis(income = true) { BarChart() }; Text("        2月                 3月                 4月                 5月", color = Color.Gray, fontSize = 10.sp) }
    Spacer(Modifier.height(12.dp)); Section("支出分类") { var categoryRange by rememberSaveable { mutableStateOf("本月") }; Row(Modifier.fillMaxWidth().background(Color(0xFFF2F2EF), RoundedCornerShape(16.dp)).padding(3.dp), horizontalArrangement = Arrangement.SpaceEvenly) { listOf("本月", "近3个月", "近1年").forEach { option -> TextButton(onClick = { categoryRange = option }, modifier = Modifier.weight(1f), colors = ButtonDefaults.textButtonColors(containerColor = if (categoryRange == option) Green else Color.Transparent, contentColor = if (categoryRange == option) Color.White else Color.Gray), shape = RoundedCornerShape(13.dp)) { Text(option, fontSize = 10.sp) } } }; Spacer(Modifier.height(8.dp)); Column(horizontalAlignment = Alignment.CenterHorizontally) { DonutChart(); Spacer(Modifier.height(6.dp)); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) { Column { Text("● 餐饮 32%", color = Color(0xFFE97865), fontSize = 11.sp); Text("● 购物 24%", color = Color(0xFF67B7DC), fontSize = 11.sp); Text("● 交通 15%", color = Color(0xFF9BCB73), fontSize = 11.sp) }; Column { Text("¥1,997", fontSize = 11.sp); Text("¥1,498", fontSize = 11.sp); Text("¥936", fontSize = 11.sp) }; Column { Text("● 日用 12%", color = Color(0xFFF6B44A), fontSize = 11.sp); Text("● 娱乐 9%", color = Color(0xFF8D8CE8), fontSize = 11.sp); Text("● 其他 8%", color = Color.Gray, fontSize = 11.sp) } } } }
}

@Composable fun ChartWithAxis(income: Boolean = false, chart: @Composable () -> Unit) { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.height(130.dp), verticalArrangement = Arrangement.SpaceBetween, horizontalAlignment = Alignment.End) { listOf(if (income) "20,000" else "220,000", if (income) "10,000" else "180,000", if (income) "5,000" else "140,000", "0").forEach { Text(it, color = Color.Gray, fontSize = 8.sp) } }; Spacer(Modifier.width(6.dp)); Box(Modifier.weight(1f)) { chart() } } }
@Composable fun LineChart() { Canvas(Modifier.fillMaxWidth().height(130.dp).padding(8.dp)) { val pts = listOf(.28f,.25f,.30f,.40f,.50f,.46f,.58f,.66f,.62f,.76f,.72f,.92f); val w=size.width; val h=size.height; for(i in 1..4) drawLine(Color(0xFFE5E8E4), androidx.compose.ui.geometry.Offset(0f,h*i/5), androidx.compose.ui.geometry.Offset(w,h*i/5), 1f); val path=androidx.compose.ui.graphics.Path(); pts.forEachIndexed { i,v -> val p=androidx.compose.ui.geometry.Offset(w*i/(pts.size-1), h*(1-v)); if(i==0) path.moveTo(p.x,p.y) else path.lineTo(p.x,p.y) }; val filled=androidx.compose.ui.graphics.Path().apply { addPath(path); lineTo(w,h); lineTo(0f,h); close() }; drawPath(filled, Brush.verticalGradient(listOf(Green.copy(.28f), Green.copy(.02f))), alpha = 1f); drawPath(path, Green, style=androidx.compose.ui.graphics.drawscope.Stroke(width=5f)); pts.forEachIndexed { i,v -> drawCircle(Green, 5f, androidx.compose.ui.geometry.Offset(w*i/(pts.size-1), h*(1-v))) } } }
@Composable fun BarChart() { Canvas(Modifier.fillMaxWidth().height(130.dp).padding(12.dp)) { val months=4; val bw=size.width/(months*3f); val vals=listOf(.78f,.82f,.72f,.62f); vals.forEachIndexed { i,v -> val x=(i*3+1)*bw; drawRect(Green, androidx.compose.ui.geometry.Offset(x,size.height*(1-v)), androidx.compose.ui.geometry.Size(bw,size.height*v)); drawRect(Color(0xFFE97865), androidx.compose.ui.geometry.Offset(x+bw+4,size.height*(1-v*.48f)), androidx.compose.ui.geometry.Size(bw,size.height*v*.48f)) } } }
@Composable fun DonutChart() { Box(Modifier.size(164.dp), contentAlignment = Alignment.Center) { Canvas(Modifier.size(164.dp)) { val colors=listOf(Color(0xFFE97865),Color(0xFF67B7DC),Color(0xFF9BCB73),Color(0xFFF6B44A),Color(0xFF8D8CE8)); var start=-90f; val sweep=360f/colors.size; colors.forEach { c -> drawArc(c,start,sweep-4f,false,style=androidx.compose.ui.graphics.drawscope.Stroke(width=72f)); start+=sweep } }; Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("¥6,240", fontWeight = FontWeight.Bold, fontSize = 16.sp); Text("本月支出", color = Color.Gray, fontSize = 11.sp) } } }
@Composable fun AdviceList(f: FinanceViewModel) = Page("建议") { Row(Modifier.fillMaxWidth().background(Color.White.copy(.72f), RoundedCornerShape(22.dp)).padding(4.dp), horizontalArrangement = Arrangement.SpaceEvenly) { Text("建议", color = Green, fontWeight = FontWeight.Bold, modifier = Modifier.background(Color.White, RoundedCornerShape(18.dp)).padding(horizontal = 54.dp, vertical = 10.dp)); Text("目标", color = Color.Gray, modifier = Modifier.padding(horizontal = 54.dp, vertical = 10.dp)) }; Spacer(Modifier.height(18.dp)); Text("我的目标", fontWeight = FontWeight.Bold, fontSize = 20.sp); GoalCard(Goal("应急备用金", 50000.0, 30000.0, LocalDate.now().plusMonths(12)), 2000.0) {}; GoalCard(Goal("购房首付", 500000.0, 120000.0, LocalDate.now().plusMonths(18)), 21111.0) {}; Spacer(Modifier.height(12.dp)); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("今日建议", fontWeight = FontWeight.Bold, fontSize = 20.sp); Text("换一批", color = Green) }; f.advice.take(2).forEach { AdviceCard(it) } }

@Composable fun Page(title: String, content: @Composable ColumnScope.() -> Unit) { LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), contentPadding = PaddingValues(top = 18.dp, bottom = 26.dp)) { item { Text(title, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Ink); Spacer(Modifier.height(4.dp)); Column(content = content) } } }
@Composable fun NetWorth(f: FinanceViewModel) { Card(colors = CardDefaults.cardColors(containerColor = Color.Transparent), shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, Color.White.copy(.42f))) { Box(Modifier.background(Brush.linearGradient(listOf(Green, GreenDark)), RoundedCornerShape(24.dp))) { Canvas(Modifier.matchParentSize().padding(start = 150.dp, top = 22.dp, end = 14.dp, bottom = 16.dp)) { val points = listOf(.18f,.28f,.23f,.42f,.36f,.54f,.47f,.68f,.61f,.90f); val path = androidx.compose.ui.graphics.Path(); points.forEachIndexed { i, value -> val x = size.width * i / (points.size - 1); val y = size.height * (1f - value); if (i == 0) path.moveTo(x, y) else path.lineTo(x, y) }; val fill = androidx.compose.ui.graphics.Path().apply { addPath(path); lineTo(size.width, size.height); lineTo(0f, size.height); close() }; drawPath(fill, Color.White.copy(alpha = .13f)); drawPath(path, Color.White.copy(alpha = .65f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)); points.forEachIndexed { i, value -> drawCircle(Color.White.copy(alpha = .78f), 4f, androidx.compose.ui.geometry.Offset(size.width * i / (points.size - 1), size.height * (1f - value))) } }; Column(Modifier.padding(22.dp)) { Text("总净资产", color = Color.White.copy(.8f)); Text("¥${money(f.netWorth)}", fontSize = 32.sp, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp)); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("资产 ¥${money(f.assets)}", color = Color.White); Text("负债 ¥${money(f.liabilities)}", color = Color.White) } } } } }
@Composable fun Section(title: String, action: String? = null, onAction: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) { Card(colors = CardDefaults.cardColors(containerColor = Glass), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, Color.White.copy(.8f))) { Column(Modifier.padding(16.dp)) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(title, fontWeight = FontWeight.Bold); action?.let { Text(it, color = Green, fontSize = 13.sp, modifier = if (onAction != null) Modifier.clickable { onAction() } else Modifier) } }; Spacer(Modifier.height(10.dp)); content() } } }
@Composable fun Metric(label: String, value: Double, color: Color) { Column { Text(label, color = Color.Gray, fontSize = 13.sp); Text("¥${money(value)}", color = color, fontWeight = FontWeight.Bold, fontSize = 18.sp) } }
@Composable fun AdviceCard(a: Advice) { var detail by remember { mutableStateOf(false) }; Card(Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { detail = true }, colors = CardDefaults.cardColors(containerColor = Mint), shape = RoundedCornerShape(16.dp)) { Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) { Icon(a.icon, null, tint = Green); Spacer(Modifier.width(10.dp)); Column { Text(a.title, fontWeight = FontWeight.Bold); Text(a.reason, fontSize = 13.sp, color = Color.DarkGray); Text("查看详情", fontSize = 13.sp, color = Green, modifier = Modifier.padding(top = 4.dp)) } } }; if (detail) AlertDialog(onDismissRequest = { detail = false }, title = { Text(a.title) }, text = { Text("${a.reason}\n\n${a.action}") }, confirmButton = { TextButton(onClick = { detail = false }) { Text("去设置") } }, dismissButton = { TextButton(onClick = { detail = false }) { Text("不再提示") } }) }
@Composable fun GoalCard(g: Goal, monthly: Double, edit: () -> Unit) { Section("储蓄目标", "编辑") { Text(g.name, fontWeight = FontWeight.Bold); LinearProgressIndicator(progress = { (g.current / g.target).toFloat() }, modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), color = Green); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("¥${money(g.current)} / ¥${money(g.target)}", fontSize = 13.sp); Text("每月建议 ¥${money(monthly)}", fontSize = 13.sp, color = Green) }; Text("目标日期 ${g.due}", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 6.dp)); TextButton(onClick = edit, contentPadding = PaddingValues(0.dp)) { Text("调整目标") } } }
@Composable fun TransactionRow(t: Transaction) { Row(Modifier.fillMaxWidth().padding(vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) { Surface(shape = RoundedCornerShape(12.dp), color = if (t.kind == TxKind.INCOME) Mint else Color(0xFFF3F1EE), modifier = Modifier.size(42.dp)) { Icon(if (t.kind == TxKind.INCOME) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward, null, tint = if (t.kind == TxKind.INCOME) Green else Red, modifier = Modifier.padding(10.dp)) }; Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(t.title, fontWeight = FontWeight.Medium); Text("${t.category} · ${t.date}", fontSize = 12.sp, color = Color.Gray) }; Text((if (t.kind == TxKind.INCOME) "+" else "-") + "¥${money(t.amount)}", color = if (t.kind == TxKind.INCOME) Green else Ink, fontWeight = FontWeight.Bold) } }
@Composable fun AccountRow(a: Account) { var detail by remember { mutableStateOf(false) }; Card(Modifier.fillMaxWidth().padding(vertical = 5.dp).clickable { detail = true }, colors = CardDefaults.cardColors(containerColor = if (a.kind.isLiability) Color(0xFFFFF0EC) else Glass), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, Color.White.copy(.85f))) { Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Surface(shape = RoundedCornerShape(14.dp), color = if (a.kind.isLiability) Color(0xFFFFDAD3) else Mint, modifier = Modifier.size(44.dp)) { Icon(when (a.kind) { AccountKind.CASH -> Icons.Default.Payments; AccountKind.BANK -> Icons.Default.AccountBalance; AccountKind.INVESTMENT -> Icons.Default.ShowChart; else -> Icons.Default.CreditCard }, null, tint = if (a.kind.isLiability) Red else Green, modifier = Modifier.padding(11.dp)) }; Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(a.name, fontWeight = FontWeight.Bold); Text("${a.kind.title} · ${if (a.kind.isLiability) "2 个账户" else "1 个账户"}", fontSize = 12.sp, color = Color.Gray) }; Text("${if (a.kind.isLiability) "-" else ""}¥${money(a.balance)}", fontWeight = FontWeight.Bold, color = if (a.kind.isLiability) Red else Ink); Icon(Icons.Default.ChevronRight, null, tint = Color.Gray) } }; if (detail) AlertDialog(onDismissRequest = { detail = false }, title = { Text(a.name) }, text = { Text("账户类型：${a.kind.title}\n当前余额：¥${money(a.balance)}\n更新日期：${a.updatedAt}") }, confirmButton = { TextButton(onClick = { detail = false }) { Text("管理账户") } }, dismissButton = { TextButton(onClick = { detail = false }) { Text("关闭") } }) }
@Composable fun Empty(title: String, hint: String) { Column(Modifier.fillMaxWidth().padding(top = 88.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.ReceiptLong, null, modifier = Modifier.size(52.dp), tint = Green); Text(title, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp)); Text(hint, color = Color.Gray, fontSize = 13.sp) } }

@Composable fun AccountDialog(close: () -> Unit, save: (String, AccountKind, Double) -> Unit) { var name by remember { mutableStateOf("") }; var amount by remember { mutableStateOf("") }; var kind by remember { mutableStateOf(AccountKind.BANK) }; AlertDialog(onDismissRequest = close, title = { Text("添加账户") }, text = { Column { OutlinedTextField(name, { name = it }, label = { Text("账户名称") }); AccountKind.entries.forEach { Row(Modifier.fillMaxWidth().clickable { kind = it }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) { RadioButton(kind == it, { kind = it }); Text(it.title) } }; OutlinedTextField(amount, { amount = it }, label = { Text("当前余额") }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal)) } }, confirmButton = { Button(onClick = { amount.toDoubleOrNull()?.let { if (name.isNotBlank()) save(name, kind, it) } }) { Text("保存") } }, dismissButton = { TextButton(onClick = close) { Text("取消") } }) }
@Composable fun TransactionDialog(close: () -> Unit, save: (String, TxKind, Double, String) -> Unit) { var name by remember { mutableStateOf("") }; var amount by remember { mutableStateOf("") }; var category by remember { mutableStateOf("日常") }; var kind by remember { mutableStateOf(TxKind.EXPENSE) }; AlertDialog(onDismissRequest = close, title = { Text("新增记录") }, text = { Column { OutlinedTextField(name, { name = it }, label = { Text("说明") }); Row { TxKind.entries.forEach { FilterChip(selected = kind == it, onClick = { kind = it }, label = { Text(it.title) }, modifier = Modifier.padding(end = 4.dp)) } }; OutlinedTextField(amount, { amount = it }, label = { Text("金额") }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal)); OutlinedTextField(category, { category = it }, label = { Text("分类") }) } }, confirmButton = { Button(onClick = { amount.toDoubleOrNull()?.let { if (name.isNotBlank()) save(name, kind, it, category) } }) { Text("保存") } }, dismissButton = { TextButton(onClick = close) { Text("取消") } }) }
@Composable fun GoalDialog(g: Goal, close: () -> Unit, save: (String, Double, Double, LocalDate) -> Unit) { var name by remember { mutableStateOf(g.name) }; var target by remember { mutableStateOf(g.target.toString()) }; var current by remember { mutableStateOf(g.current.toString()) }; AlertDialog(onDismissRequest = close, title = { Text("设置储蓄目标") }, text = { Column { OutlinedTextField(name, { name = it }, label = { Text("目标名称") }); OutlinedTextField(target, { target = it }, label = { Text("目标金额") }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal)); OutlinedTextField(current, { current = it }, label = { Text("当前已存") }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal)); Text("目标日期暂设为 ${g.due}", fontSize = 12.sp, color = Color.Gray) } }, confirmButton = { Button(onClick = { val t = target.toDoubleOrNull(); val c = current.toDoubleOrNull(); if (t != null && c != null && name.isNotBlank()) save(name, t, c, g.due) }) { Text("保存") } }, dismissButton = { TextButton(onClick = close) { Text("取消") } }) }
fun money(value: Double) = "%,.0f".format(value)
