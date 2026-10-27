# GitHub 同类项目调研与实施记录

调研日期：2026-10-27。通过 GitHub 连接器读取仓库 README、许可证及相关组件源码。

| 项目 | 许可证 | 适合借鉴的部分 | 本项目实施方式 |
| --- | --- | --- | --- |
| [Compose-Expense](https://github.com/wisnukurniawan/Compose-Expense) | Apache-2.0 | Compose 记账流程，Room、DataStore、ViewModel 分层 | 作为财务功能与后续持久化架构参考 |
| [AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass) | Apache-2.0 | Backdrop 背景采样、模糊、lens 折射与弹性选中态 | 阅读 LiquidBottomTabs 源码；当前先实现适配现有 Compose 的浮动导航 |
| [compose-floating-tab-bar](https://github.com/elyesmansour/compose-floating-tab-bar) | Apache-2.0 | 浮动胶囊导航、滚动状态、Haze 模糊接入 | 借鉴布局和交互形式，自行编写兼容组件 |

## 本轮落地

- 将固定 Material 底部栏替换为悬浮胶囊，保留系统手势安全区。
- 使用一个连续运动的选中背景与弹簧动画，替代每个按钮独立出现的选中块。
- 图标随选中状态缩放和变色；导航项提供 Tab 角色及选中语义。
- 高光描边、柔和阴影及浅绿渐变提供玻璃层次。

当前组件是独立编写的实现，没有复制上述项目源码或引入其二进制依赖。当前的半透明高光尚不包含真实背景折射；要达到 Backdrop 示例的光学效果，仍需完成 Compose/Kotlin 兼容性升级及 GPU 性能验证。

## 后续完整目标

1. 引入并验证 Backdrop 或 Haze 的背景采样效果，保留低性能设备降级路径。
2. 将内存数据迁移为持久化仓库，让账户、交易、目标在重启后保留。
3. 将统计周期连接真实数据，避免仅切换选中样式。
4. 逐项验证账户选择、保存提示、目标编辑、统计详情等交互，并记录回归结果。

若后续复用第三方源码，将保留其版权、许可证及修改说明。
