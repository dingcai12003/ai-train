# JetLinks AI 开发全流程演示包

## 结论

这个目录已经整理成一套可直接演示的材料，主题是“基于 JetLinks 脚手架，用 AI 从需求文档一路推进到开发、测试与上线”。

本次演示选择的业务场景是“冷链设备监控与告警处置”，原因有 3 个：

1. JetLinks 原生就覆盖设备接入、规则引擎、通知、时序数据这些核心能力。
2. 业务画面直观，容易讲清楚“平台配置能力”和“AI 辅助二开能力”的边界。
3. 可以额外追加一个很小但很完整的二开点：`告警处置台账`，适合演示 AI 写前后端代码。

## 当前目录说明

```text
演示/
├── README.md
├── docs/
│   ├── 00-演示总览.md
│   ├── 01-需求文档-冷链设备监控与告警处置.md
│   ├── 02-技术方案与模块落点.md
│   ├── 03-AI开发流程与提示词.md
│   ├── 04-环境准备与启动说明.md
│   ├── 05-测试与上线清单.md
│   └── 06-现场演示脚本.md
├── jetlinks-community/
├── jetlinks-ui-vue/
└── device-simulator/
```

## 已准备的仓库

以下仓库在 2026-04-29 已核对过 GitHub 信息：

1. `jetlinks-community`
   仓库：<https://github.com/jetlinks/jetlinks-community>
   用途：后端平台基座，默认分支 `2.11`。

2. `jetlinks-ui-vue`
   仓库：<https://github.com/jetlinks/jetlinks-ui-vue>
   用途：前端基座，默认分支 `2.11`。

3. `device-simulator`
   仓库：<https://github.com/jetlinks/device-simulator>
   用途：设备模拟器，用来在没有真实硬件时演示设备上报。

说明：`jetlinks-ui-vue` 依赖多个 Git 子模块。为了避免 SSH Key 成为演示阻塞点，这里优先采用源码下载方式把主仓库和关键模块目录落到本地。

## 推荐阅读顺序

1. 先看 `docs/00-演示总览.md`
2. 再看 `docs/01-需求文档-冷链设备监控与告警处置.md`
3. 然后讲 `docs/02-技术方案与模块落点.md`
4. 进入 `docs/03-AI开发流程与提示词.md`
5. 需要现场启动时，再打开 `docs/04-环境准备与启动说明.md`
6. 收尾用 `docs/05-测试与上线清单.md` 与 `docs/06-现场演示脚本.md`

## 当前机器环境

已确认到的本机工具版本如下：

1. Java：`21.0.9`
2. Maven：`3.9.9`，当前 `mvn` 实际使用的 JDK 是 `23.0.2`
3. Node.js：`22.16.0`
4. pnpm：`10.28.2`
5. Docker：`29.1.3`

注意：

1. `jetlinks-community` README 标注推荐 `Java 17`。
2. `jetlinks-ui-vue` 的 `package.json` 标注 `node >= 22.18.0`。
3. 所以如果你要现场真正启动，建议先切到匹配版本再跑。

## 验证方法

你可以先执行下面 3 个检查，确认材料目录是否完整：

```bash
ls
find docs -maxdepth 1 -type f | sort
find jetlinks-community jetlinks-ui-vue -maxdepth 2 -type d | head -n 40
```
