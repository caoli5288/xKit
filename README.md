# xKit [![Build Status](http://ci.mengcraft.com:8080/job/xKit/badge/icon)](http://ci.mengcraft.com:8080/job/xKit/)
A kit manager for bukkit based minecraft server.
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
- /xkit set <kit_name> command \[command]...
  - 设置领取同时后台执行命令
  - 格式为JSON数组
  - 示例"/xkit set kit1 command ["kill %player%", "kick %player%"]"
  - %player%被替换成玩家名
- /xkit set <kit_name> permission \[permission]
  - 设置领取所需权限
- /xkit kit <kit_name>