# xKit [![Build Status](http://ci.mengcraft.com:8080/job/xKit/badge/icon)](http://ci.mengcraft.com:8080/job/xKit/)
A kit manager for bukkit based minecraft server. Release under GPLv2.

## 指令
- /xkit all
  - 检视所有已定义的礼包
- /xkit add <kit_name>
  - 定义新礼包
- /xkit del <kit_name>
  - 删除已定义的礼包
- /xkit set <kit_name>
  - 设置礼包内含物品
- /xkit set <kit_name> period \[period_time]
  - 设置礼包领取间隔（秒）
- /xkit set <kit_name> day \[period_day]
  - 设置礼包领取间隔（天）
- /xkit set <kit_name> command \[command]...
  - 设置领取同时后台执行命令
  - 格式为JSON数组
  - 示例"/xkit set kit1 command \["kill %player%", "kick %player%"]
  - %player%被替换成玩家名
- /xkit set <kit_name> permission \[permission]
  - 设置领取所需权限
- /xkit kit <kit_name> \[player_name]
  - 领取指定礼包。当后接玩家名时，可以使特定玩家强制领取而无视间隔和权限

## Placeholder
- xkit_<kit_name>_okay
  - return true if player's <kit_name> not in cooling time. Other wise false.
- xkit_<kit_name>_next
  - return cooling time remained by second. Or -1 if not in cooling time.
- xkit_<kit_name>_nextdate
  - return cooling time ending date. Or null if not in cooling time.
  - return null if named kit not exist.
