# S1 Discord Server CMD

Fabric 1.19.4 サーバーサイドmod。  
Discord BotからMCRCON経由でMinecraftサーバーコマンドを**安全に**実行するためのゲートウェイ。

## 特徴

- サーバー上の全コマンドをスキャンしてJSON形式で取得可能（Discord Botのスラッシュコマンド自動登録に利用）
- Discord Role IDベースの権限管理（ホワイトリスト方式）
- 危険コマンドのブラックリスト機能（`/stop`、`/op`等をデフォルトブロック）
- 全コマンドが権限レベル4（RCON/コンソール専用）なので一般プレイヤーからは実行不可

## 導入

1. [Releases](https://github.com/ASTRAL-SMP/S1DiscordServerCMD/releases) から `s1-discord-server-cmd-x.x.x.jar` をダウンロード
2. サーバーの `mods/` フォルダに配置
3. [Fabric API](https://modrinth.com/mod/fabric-api) も `mods/` に配置
4. サーバーを起動

### 前提

| 項目 | バージョン |
|------|-----------|
| Minecraft | 1.19.4 |
| Fabric Loader | >= 0.14.19 |
| Fabric API | 必須 |
| Java | >= 17 |

## 生成ファイル

### `world/data/s1discordcmd_permissions.dat`

権限データの永続化ファイル（NBT形式）。サーバー停止時に自動保存される。  
以下のデータが格納される:

| キー | 内容 |
|------|------|
| `roles` | Discord Role ID ごとの許可コマンドセット |
| `blacklist` | 実行がブロックされるコマンドの一覧 |

> このファイルは直接編集せず、後述のコマンドで管理する。

### デフォルトブラックリスト

初回起動時に以下のコマンドが自動的にブラックリストに登録される:

```
stop, op, deop, ban-ip, pardon-ip, save-all, save-off, save-on, debug
```

## コマンド一覧

全コマンドは `/s1` プレフィックス配下。権限レベル4（RCON/コンソール）が必要。

| コマンド | 説明 |
|---------|------|
| `/s1 commands` | サーバー全コマンドをJSON配列で返す |
| `/s1 permit <roleId> <command>` | ロールにコマンド実行を許可 |
| `/s1 deny <roleId> <command>` | ロールから許可を取り消し |
| `/s1 permissions [roleId]` | 権限一覧（roleId省略で全ロール表示） |
| `/s1 exec <roleId> <command...>` | 権限チェック付きでコマンド実行 |
| `/s1 blacklist list` | ブラックリスト一覧 |
| `/s1 blacklist add <command>` | ブラックリストに追加 |
| `/s1 blacklist remove <command>` | ブラックリストから削除 |

## 設定ガイド

### 1. server.properties でRCONを有効化

```properties
enable-rcon=true
rcon.port=25575
rcon.password=ここに強力なパスワードを設定
```

### 2. ブラックリストの確認・調整

サーバーコンソールまたはRCONから:

```
# 現在のブラックリストを確認
/s1 blacklist list

# 必要に応じて追加（例: kickもブロックしたい場合）
/s1 blacklist add kick

# 信頼できる環境でsave-allを許可したい場合
/s1 blacklist remove save-all
```

### 3. Discord Roleに権限を付与

Discord Role IDは、Discordの開発者モードを有効にしてロールを右クリック → 「IDをコピー」で取得できる。

```
# 管理者ロール（例: 123456789）に広い権限を付与
/s1 permit 123456789 whitelist
/s1 permit 123456789 ban
/s1 permit 123456789 kick
/s1 permit 123456789 say
/s1 permit 123456789 gamemode
/s1 permit 123456789 tp

# モデレーターロール（例: 987654321）には限定的な権限
/s1 permit 987654321 say
/s1 permit 987654321 whitelist

# 一般メンバーロール（例: 111222333）にはsayのみ
/s1 permit 111222333 say
```

> `*` をコマンド名に指定すると全コマンドを許可できるが、ブラックリストは依然有効。

```
# 管理者にブラックリスト以外の全コマンドを許可
/s1 permit 123456789 *
```

### 4. 権限の確認

```
# 全ロールの権限を確認
/s1 permissions

# 特定ロールの権限を確認
/s1 permissions 123456789
```

### 5. 権限の取り消し

```
/s1 deny 987654321 kick
```

## Discord Botからの利用

Discord Botは以下のフローでコマンドを実行する:

### コマンド一覧の取得

Bot起動時にRCON経由で `/s1 commands` を実行し、返却されるJSON配列をパースしてスラッシュコマンドを登録する。

```
RCON → /s1 commands
← ["advancement","attribute","ban","ban-ip","clear","clone",...,"whitelist"]
```

### コマンドの実行

ユーザーがDiscord上でコマンドを実行した際、Botはユーザーの最上位ロールIDを使って `/s1 exec` を呼ぶ。

```
RCON → /s1 exec 123456789 say Hello from Discord!
← OK: Executed 'say Hello from Discord!' for role 123456789
```

権限がない場合:

```
RCON → /s1 exec 111222333 ban PlayerName
← DENIED: Role 111222333 is not permitted to execute: ban PlayerName
```

### 推奨実装パターン

```
1. Bot起動時: /s1 commands でコマンド一覧取得
2. 各コマンドをDiscordスラッシュコマンドとして登録
3. ユーザーがコマンド実行時:
   a. ユーザーのDiscordロールIDを取得
   b. /s1 exec <roleId> <command> をRCONで送信
   c. 応答を確認（"OK:" or "DENIED:"）
   d. 結果をDiscordに返信
```

## セキュリティに関する注意

- **デフォルト拒否**: `permit` されていないコマンドは一切実行できない
- **ブラックリスト最優先**: ブラックリストのコマンドは `permit *` でも実行不可
- **RCONパスワード**: 必ず強力なパスワードを設定し、外部に漏洩しないよう管理する
- **ネットワーク**: RCONポートをファイアウォールで制限し、Discord Botが稼働するホストからのみアクセスを許可する

## ビルド

```bash
./gradlew build
```

生成物: `build/libs/s1-discord-server-cmd-x.x.x.jar`
