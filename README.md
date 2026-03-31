# S1 Discord Server CMD

Fabric 1.19.4 サーバーサイドmod。  
Discord Bot内蔵で、Discordのスラッシュコマンドから直接Minecraftサーバーコマンドを実行できる。

## 特徴

- **Discord Bot内蔵**: modにDiscord Botが組み込まれており、別途Botを用意する必要がない
- **スラッシュコマンド `/mc`**: Discordから `/mc command:say args:Hello` のようにサーバーコマンドを実行
- **オートコンプリート**: コマンド入力時にロールに応じた実行可能コマンドが候補表示される
- **ロールベース権限管理**: Discord Role IDごとに実行可能コマンドをホワイトリスト方式で管理
- **危険コマンドブラックリスト**: `/stop`、`/op`等はデフォルトブロック、permitしても実行不可
- **コンソール管理コマンド `/s1`**: サーバーコンソールから権限の設定・確認が可能

## 導入

1. [Releases](https://github.com/ASTRAL-SMP/S1DiscordServerCMD/releases) から `s1-discord-server-cmd-x.x.x.jar` をダウンロード
2. サーバーの `mods/` フォルダに配置
3. [Fabric API](https://modrinth.com/mod/fabric-api) も `mods/` に配置
4. サーバーを一度起動して設定ファイルを生成させる
5. 設定ファイルを編集して再起動

### 前提

| 項目 | バージョン |
|------|-----------|
| Minecraft | 1.19.4 |
| Fabric Loader | >= 0.14.19 |
| Fabric API | 必須 |
| Java | >= 17 |

## 生成ファイル

### `config/s1discordcmd.json`

初回起動時に自動生成される設定ファイル。

```json
{
  "botToken": "",
  "guildId": ""
}
```

| キー | 説明 |
|------|------|
| `botToken` | Discord Bot のトークン |
| `guildId` | Botを動作させるDiscordサーバー（Guild）のID |

### `world/data/s1discordcmd_permissions.dat`

権限データの永続化ファイル（NBT形式）。サーバー停止時に自動保存される。

| キー | 内容 |
|------|------|
| `roles` | Discord Role ID ごとの許可コマンドセット |
| `blacklist` | 実行がブロックされるコマンドの一覧 |

> このファイルは直接編集せず、`/s1` コマンドで管理する。

### デフォルトブラックリスト

初回起動時に以下のコマンドが自動的にブラックリストに登録される:

```
stop, op, deop, ban-ip, pardon-ip, save-all, save-off, save-on, debug
```

## 設定ガイド

### 1. Discord Bot を作成する

1. [Discord Developer Portal](https://discord.com/developers/applications) でアプリケーションを作成
2. 「Bot」タブでBotを作成し、**Token** をコピー
3. 「OAuth2」→「URL Generator」で以下のスコープを選択:
   - `bot`
   - `applications.commands`
4. Bot Permissions で必要な権限を選択（最低限 `Send Messages`）
5. 生成されたURLでBotをサーバーに招待

### 2. config を設定する

サーバーを一度起動すると `config/s1discordcmd.json` が生成される。

```json
{
  "botToken": "MTIzNDU2Nzg5.xxxxx.yyyyy",
  "guildId": "123456789012345678"
}
```

- **botToken**: 手順1でコピーしたトークン
- **guildId**: Discordの開発者モードを有効にしてサーバーアイコンを右クリック →「IDをコピー」

設定後、サーバーを再起動するとBotが接続される。

### 3. ブラックリストの確認・調整

サーバーコンソールから:

```
# 現在のブラックリストを確認
s1 blacklist list

# 必要に応じて追加（例: kickもブロックしたい場合）
s1 blacklist add kick

# 信頼できる環境でsave-allを許可したい場合
s1 blacklist remove save-all
```

### 4. Discord Roleに権限を付与

Discord Role IDは、開発者モードでロールを右クリック →「IDをコピー」で取得。

```
# 管理者ロール（例: 123456789）に広い権限を付与
s1 permit 123456789 whitelist
s1 permit 123456789 ban
s1 permit 123456789 kick
s1 permit 123456789 say
s1 permit 123456789 gamemode
s1 permit 123456789 tp

# モデレーターロール（例: 987654321）には限定的な権限
s1 permit 987654321 say
s1 permit 987654321 whitelist

# 一般メンバーロール（例: 111222333）にはsayのみ
s1 permit 111222333 say
```

> `*` を指定すると全コマンドを許可（ブラックリストは依然有効）:
> ```
> s1 permit 123456789 *
> ```

### 5. 権限の確認・取り消し

```
# 全ロールの権限を確認
s1 permissions

# 特定ロールの権限を確認
s1 permissions 123456789

# 権限を取り消し
s1 deny 987654321 kick
```

## Discord上での使い方

### `/mc` スラッシュコマンド

BotがDiscordサーバーに接続すると `/mc` コマンドが登録される。

| オプション | 必須 | 説明 |
|-----------|------|------|
| `command` | はい | 実行するコマンド名（オートコンプリート付き） |
| `args` | いいえ | コマンド引数 |

#### 使用例

```
/mc command:say args:Hello from Discord!
/mc command:whitelist args:add PlayerName
/mc command:gamemode args:creative PlayerName
```

入力中にオートコンプリートが表示され、自分のロールで実行可能なコマンドのみが候補に出る。

#### 権限がない場合

```
/mc command:ban args:PlayerName
→ Permission denied: `ban`   (自分だけに表示)
```

## コンソールコマンド一覧（/s1）

サーバーコンソールまたはRCON用。権限レベル4が必要。

| コマンド | 説明 |
|---------|------|
| `s1 commands` | サーバー全コマンドをJSON配列で返す |
| `s1 permit <roleId> <command>` | ロールにコマンド実行を許可 |
| `s1 deny <roleId> <command>` | ロールから許可を取り消し |
| `s1 permissions [roleId]` | 権限一覧 |
| `s1 exec <roleId> <command...>` | 権限チェック付きコマンド実行（RCON用） |
| `s1 blacklist list` | ブラックリスト一覧 |
| `s1 blacklist add <command>` | ブラックリストに追加 |
| `s1 blacklist remove <command>` | ブラックリストから削除 |

## セキュリティ

- **デフォルト拒否**: `permit` されていないコマンドは一切実行できない
- **ブラックリスト最優先**: ブラックリストのコマンドは `permit *` でも実行不可
- **botToken**: 絶対にGitにコミットしたり第三者に共有しない。漏洩した場合はDiscord Developer Portalで即座にリセット

## ビルド

```bash
./gradlew build
```

生成物: `build/libs/s1-discord-server-cmd-x.x.x.jar`
