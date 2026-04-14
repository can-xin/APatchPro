# APatchPro

- 本项目是基于 APatch 的修改版本，意旨在修复原本项目的安全问题，并增加更多功能

# 项目说明

APatchPro 用于 Android 内核与系统补丁管理，提供：

- 基于内核的 Root 方案
- APM（类 Magisk 的模块能力）
- KPM（内核模块注入能力，支持 inline-hook / syscall-table-hook）

对比原有项目，修改或增加了以下内容：

- 修复了单因素鉴权导致的不安全问题
- 增加了设置被授权应用的Linux Capabilities单独配置功能
- 增加了双key鉴权读写分离机制，并且将2key作为hash写入内核，避免了原项目明文存储superkey到内核导致的安全问题
- 增加了magisk模块安装前的静态规则扫描
- 增加了管理器进入前的生物识别认证功能
- 增加了修改管理器全局背景图片的功能

# 支持范围

- 仅支持 ARM64 架构
- 仅支持 Android 内核 3.18 - 6.12

# 内核要求

- CONFIG_KALLSYMS=y 且 CONFIG_KALLSYMS_ALL=y

# 安全提示

- 在该版本，虽然加入了双key鉴权，并且2key作为hash写入内核，但仍然是密钥认证，因此仍有安全风险
- 由于原项目大多数功能与superkey深度绑定，导致在我们试图修改全新的鉴权方式受到了巨大阻力，因此不得不妥协了许多内容，比如1key仍然被作为明文储存在内核，并且管理器。依然依靠androidkeystore加密superkey，并且superkey依然有拉起rootshell的能力，因为这和模块能力直接相关，但是1key原有的能力已经被限制，除拉起rootshell外，其余都为只读，这大大的降低了恶意root读取到1key带来的风险

# 杂谈

- 现在这个项目仍处于beta阶段，我都快要被修改2key后带来的一系列问题给逼疯了😭，比如magisk模块这个问题之类的……额……内核补丁方面到是完工了，这个管理器就不咋样了，哈哈哈哈（，所以总之， 一堆问题我也懒得测了😡，如果使用时出现了问题请到issues反馈喵，谢谢喵，magisk模块目前用不了是已知的，这个不用反馈（，然后是项目本身，也是改这那改的我都迷糊了，纯属闲来没事写的东西，以及chatgpt5.3codex有点难绷，总是改出点问题来浪费我的高级请求

# 致谢

- [KernelPatch](https://github.com/can-xin/KernelPatchModded)（核心能力来源）
- [APatch](https://github.com/bmax121/APatch)（上游基础项目）
- [KernelSU](https://github.com/tiann/KernelSU)（部分 UI 与模块机制参考）
- [Magisk](https://github.com/topjohnwu/Magisk)（相关策略与实现参考）

# 许可证

本项目遵循 GNU General Public License v3.0（GPL-3.0），详见 LICENSE。
