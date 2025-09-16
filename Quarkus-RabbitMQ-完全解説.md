# Quarkus RabbitMQ 見積もりシステム - 完全解説

## 📋 今日の作業内容

### 🎯 目標
Quarkus RabbitMQガイド（https://ja.quarkus.io/guides/rabbitmq）に基づいて、非同期メッセージングを使用した見積もりシステムを構築する。

### 🔄 作業の流れ
1. **環境調査** → PC環境の確認
2. **Quarkusインストール** → JBangを使用してQuarkus CLIをインストール
3. **プロジェクト作成** → Gradle Kotlin DSLでプロジェクト生成
4. **Java実装** → KotlinからJavaに変更（ビルドエラー回避）
5. **RabbitMQ設定** → Docker ComposeでRabbitMQ起動
6. **アプリケーション実装** → Producer/Processorアプリケーション作成
7. **問題解決** → 設定エラー、ClassCastException等を解決
8. **HTML UI作成** → 見積もり依頼・結果表示用Webページ
9. **SSE実装試行** → Server-Sent Events機能追加（失敗）
10. **ポーリング実装** → SSEの代替としてポーリング機能実装

---

## 🚫 詰まったポイントと原因

### 1. **Kotlinビルドエラー**
**問題**: Kotlinでプロジェクトを作成したが、ビルド時にエラーが発生
**原因**: QuarkusのKotlinサポートが不完全で、依存関係の解決に問題
**解決**: Javaに変更してプロジェクトを再作成

### 2. **RabbitMQ設定エラー**
**問題**: `Unrecognized configuration key "amqp.host"`
**原因**: 設定キーが間違っていた（`amqp` → `rabbitmq`）
**解決**: 設定ファイルのキーを修正

### 3. **ClassCastException**
**問題**: `io.vertx.core.json.JsonObject cannot be cast to class org.acme.QuoteRequest`
**原因**: JSONシリアライゼーション設定が不完全
**解決**: 手動でJsonObjectからPOJOに変換

### 4. **TooManyUpstreamCandidatesException**
**問題**: `'mp.messaging.outgoing.quotes.merge=true' to allow multiple upstreams`
**原因**: 複数の上流コンポーネントが同じチャンネルに接続
**解決**: Emitterパターンに変更してmerge設定を削除

### 5. **SSE実装失敗**
**問題**: Server-Sent Events機能でコンパイルエラー
**原因**: 不明なコネクタ（`smallrye-reactive-streams`）の使用
**解決**: SSE機能を削除してポーリング方式に変更

---

## 🏗️ システム全体の流れ

```
[ユーザー] → [HTMLページ] → [Producer API] → [RabbitMQ] → [Processor] → [RabbitMQ] → [Producer] → [HTMLページ] → [ユーザー]
```

### 詳細フロー
1. **見積もり依頼**: ユーザーがHTMLページで商品名を入力
2. **API送信**: ProducerのREST APIにPOSTリクエスト
3. **メッセージ送信**: ProducerがRabbitMQにQuoteRequestメッセージを送信
4. **メッセージ受信**: ProcessorがRabbitMQからQuoteRequestを受信
5. **価格計算**: Processorがランダムな価格を生成
6. **結果送信**: ProcessorがRabbitMQにQuoteメッセージを送信
7. **結果受信**: ProducerがRabbitMQからQuoteを受信
8. **結果保存**: Producerがメモリ内にQuoteを保存
9. **結果表示**: HTMLページがポーリングで結果を取得・表示

---

## 💻 使用したコードの完全解説

### 📁 プロジェクト構造
```
code-with-quarkus-6/
├── rabbitmq-quickstart-producer/     # 見積もり依頼受付・結果表示アプリ
│   ├── src/main/java/org/acme/
│   │   ├── Quote.java               # 見積もり結果データクラス
│   │   ├── QuoteRequest.java        # 見積もり依頼データクラス
│   │   ├── QuoteResource.java       # REST APIエンドポイント
│   │   └── QuoteProcessor.java      # RabbitMQメッセージ処理
│   ├── src/main/resources/
│   │   ├── application.properties   # 設定ファイル
│   │   └── META-INF/resources/
│   │       └── quotes.html          # Web UI
│   └── build.gradle.kts            # Gradleビルド設定
└── rabbitmq-quickstart-processor/   # 見積もり処理アプリ
    ├── src/main/java/org/acme/
    │   ├── Quote.java               # 見積もり結果データクラス
    │   ├── QuoteRequest.java       # 見積もり依頼データクラス
    │   └── QuoteProcessor.java      # RabbitMQメッセージ処理
    ├── src/main/resources/
    │   └── application.properties   # 設定ファイル
    └── build.gradle.kts            # Gradleビルド設定
```

---

## 🔧 各ファイルの詳細解説

### 1. **Quote.java** (データクラス)
```java
package org.acme;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * 見積もり結果を表すデータクラス
 * RabbitMQで送受信される見積もり結果の情報を保持
 */
public class Quote {
    private UUID id;              // 見積もり結果の一意ID
    private UUID requestId;        // 元の依頼ID（リクエストと結果を紐づけ）
    private String product;        // 商品名
    private BigDecimal price;      // 見積もり価格
    private Instant timestamp;    // 見積もり作成時刻

    // コンストラクタ、ゲッター、セッター、toString等
}
```

**役割**: 見積もり結果のデータ構造を定義。JSONシリアライゼーションに対応。

### 2. **QuoteRequest.java** (データクラス)
```java
package org.acme;

import java.util.UUID;

/**
 * 見積もり依頼を表すデータクラス
 * RabbitMQで送信される見積もり依頼の情報を保持
 */
public class QuoteRequest {
    private UUID id;        // 依頼の一意ID
    private String product;  // 商品名

    // コンストラクタ、ゲッター、セッター、toString等
}
```

**役割**: 見積もり依頼のデータ構造を定義。ProducerからProcessorに送信される。

### 3. **QuoteResource.java** (Producer - REST API)
```java
package org.acme;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import java.util.Map;
import java.util.UUID;

/**
 * 見積もり依頼のREST APIエンドポイント
 * HTTPリクエストを受けてRabbitMQにメッセージを送信
 */
@Path("/quotes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class QuoteResource {

    @Inject
    @Channel("quote-requests")
    Emitter<QuoteRequest> quoteRequestEmitter;  // RabbitMQ送信用

    @Inject
    QuoteProcessor quoteProcessor;  // 見積もり結果取得用

    /**
     * 見積もり依頼を送信
     * POST /quotes/request
     */
    @POST
    @Path("/request")
    public Map<String, Object> requestQuote(QuoteRequest request) {
        UUID requestId = UUID.randomUUID();
        request.setId(requestId);
        
        // RabbitMQにメッセージを送信
        quoteRequestEmitter.send(request);
        
        return Map.of("requestId", requestId);
    }

    /**
     * 指定された依頼IDの見積もり結果を取得
     * GET /quotes/result/{requestId}
     */
    @GET
    @Path("/result/{requestId}")
    public Quote getQuoteResult(@PathParam("requestId") UUID requestId) {
        return quoteProcessor.getQuote(requestId);
    }

    /**
     * すべての見積もり結果を取得
     * GET /quotes/results
     */
    @GET
    @Path("/results")
    public Map<UUID, Quote> getAllQuoteResults() {
        return quoteProcessor.getAllQuotes();
    }
}
```

**役割**: 
- HTTPリクエストを受けて見積もり依頼をRabbitMQに送信
- 見積もり結果をREST APIで提供
- HTMLページからのAJAXリクエストに対応

### 4. **QuoteProcessor.java** (Producer - メッセージ処理)
```java
package org.acme;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;
import io.vertx.core.json.JsonObject;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 見積もり結果を受信し、管理するクラス
 * RabbitMQから見積もり結果を受け取り、メモリ内に保存
 */
@ApplicationScoped
public class QuoteProcessor {

    private static final Logger LOG = Logger.getLogger(QuoteProcessor.class);

    // 見積もり結果を管理するマップ（Key: 依頼ID, Value: 見積もり結果）
    private final Map<UUID, Quote> quotes = new ConcurrentHashMap<>();

    /**
     * RabbitMQから見積もり結果を受信
     * @Incoming("quotes") - application.propertiesで定義されたチャンネル
     */
    @Incoming("quotes")
    public void processQuote(JsonObject jsonMessage) {
        LOG.info("JSONメッセージを受信: " + jsonMessage);

        try {
            // JsonObjectからQuoteオブジェクトに手動変換
            Quote quote = new Quote(
                UUID.fromString(jsonMessage.getString("id")),
                UUID.fromString(jsonMessage.getString("requestId")),
                jsonMessage.getString("product"),
                new BigDecimal(jsonMessage.getString("price")),
                Instant.parse(jsonMessage.getString("timestamp"))
            );

            LOG.info("見積もり結果を受信: " + quote);
            
            // 見積もり結果をマップに保存
            quotes.put(quote.getRequestId(), quote);

        } catch (Exception e) {
            LOG.error("見積もり結果の処理中にエラーが発生: " + e.getMessage(), e);
        }
    }

    /**
     * 指定された依頼IDの見積もり結果を取得
     */
    public Quote getQuote(UUID requestId) {
        return quotes.get(requestId);
    }

    /**
     * すべての見積もり結果を取得
     */
    public Map<UUID, Quote> getAllQuotes() {
        return quotes;
    }
}
```

**役割**:
- RabbitMQから見積もり結果を受信
- JsonObjectを手動でQuoteオブジェクトに変換（ClassCastException回避）
- 見積もり結果をメモリ内に保存・管理

### 5. **QuoteProcessor.java** (Processor - メッセージ処理)
```java
package org.acme;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.jboss.logging.Logger;
import io.vertx.core.json.JsonObject;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Random;
import java.util.UUID;

/**
 * 見積もり依頼を処理し、見積もり結果を生成するクラス
 * RabbitMQから見積もり依頼を受信し、価格を計算して結果を送信
 */
@ApplicationScoped
public class QuoteProcessor {

    private static final Logger LOG = Logger.getLogger(QuoteProcessor.class);
    private final Random random = new Random();

    @Inject
    @Channel("quotes")
    Emitter<Quote> quoteEmitter;  // 見積もり結果送信用

    /**
     * RabbitMQから見積もり依頼を受信
     * @Incoming("quote-requests") - application.propertiesで定義されたチャンネル
     */
    @Incoming("quote-requests")
    public void processQuoteRequest(JsonObject jsonMessage) {
        LOG.info("JSONメッセージを受信: " + jsonMessage);

        try {
            // JsonObjectからQuoteRequestオブジェクトに手動変換
            QuoteRequest request = new QuoteRequest(
                UUID.fromString(jsonMessage.getString("id")),
                jsonMessage.getString("product")
            );

            LOG.info("見積もり依頼を受信: " + request);

            // ランダムな価格を生成（1000-2000円の範囲）
            BigDecimal price = BigDecimal.valueOf(1000 + random.nextDouble() * 1000)
                .setScale(2, BigDecimal.ROUND_HALF_UP);

            // 見積もり結果を作成
            Quote quote = new Quote(
                UUID.randomUUID(),  // 新しいID
                request.getId(),    // 元の依頼ID
                request.getProduct(),
                price,
                Instant.now()
            );

            LOG.info("見積もり結果を生成: " + quote);

            // RabbitMQに見積もり結果を送信
            quoteEmitter.send(quote);

        } catch (Exception e) {
            LOG.error("見積もり依頼の処理中にエラーが発生: " + e.getMessage(), e);
        }
    }
}
```

**役割**:
- RabbitMQから見積もり依頼を受信
- ランダムな価格を生成
- 見積もり結果をRabbitMQに送信

### 6. **application.properties** (Producer設定)
```properties
# Quarkus RabbitMQ Producer設定

# RabbitMQ接続設定
rabbitmq.host=localhost
rabbitmq.port=5672
rabbitmq.username=guest
rabbitmq.password=guest
rabbitmq.virtual-host=/

# 見積もり依頼送信用チャンネル設定
mp.messaging.outgoing.quote-requests.connector=smallrye-rabbitmq
mp.messaging.outgoing.quote-requests.exchange.name=quote-requests
mp.messaging.outgoing.quote-requests.exchange.type=topic
mp.messaging.outgoing.quote-requests.routing-key=quote.request
mp.messaging.outgoing.quote-requests.serializer=json

# 見積もり結果受信用チャンネル設定
mp.messaging.incoming.quotes.connector=smallrye-rabbitmq
mp.messaging.incoming.quotes.queue.name=quotes
mp.messaging.incoming.quotes.queue.durable=true
mp.messaging.incoming.quotes.queue.auto-delete=false
mp.messaging.incoming.quotes.serializer=json

# ログ設定
quarkus.log.level=INFO
quarkus.log.category."org.acme".level=DEBUG
```

**役割**:
- RabbitMQ接続情報の設定
- メッセージングチャンネルの設定
- JSONシリアライゼーションの有効化

### 7. **application.properties** (Processor設定)
```properties
# Quarkus RabbitMQ Processor設定

# RabbitMQ接続設定
rabbitmq.host=localhost
rabbitmq.port=5672
rabbitmq.username=guest
rabbitmq.password=guest
rabbitmq.virtual-host=/

# 見積もり依頼受信用チャンネル設定
mp.messaging.incoming.quote-requests.connector=smallrye-rabbitmq
mp.messaging.incoming.quote-requests.queue.name=quote-requests
mp.messaging.incoming.quote-requests.queue.durable=true
mp.messaging.incoming.quote-requests.queue.auto-delete=false
mp.messaging.incoming.quote-requests.serializer=json

# 見積もり結果送信用チャンネル設定
mp.messaging.outgoing.quotes.connector=smallrye-rabbitmq
mp.messaging.outgoing.quotes.exchange.name=quotes
mp.messaging.outgoing.quotes.exchange.type=topic
mp.messaging.outgoing.quotes.routing-key=quote.result
mp.messaging.outgoing.quotes.serializer=json

# ログ設定
quarkus.log.level=INFO
quarkus.log.category."org.acme".level=DEBUG
```

### 8. **quotes.html** (Web UI)
```html
<!DOCTYPE html>
<html lang="ja">
<head>
    <meta charset="UTF-8">
    <title>Quarkus RabbitMQ 見積もりシステム</title>
    <!-- CSS省略 -->
</head>
<body>
    <div class="container">
        <h1>🚀 Quarkus RabbitMQ 見積もりシステム</h1>
        
        <div class="form-group">
            <label for="productName">商品名を入力してください:</label>
            <input type="text" id="productName" placeholder="例: iPhone 15 Pro, MacBook Air, etc.">
        </div>
        
        <button onclick="requestQuote()" id="requestButton">見積もり依頼を送信</button>
        
        <div id="status" class="status" style="display: none;"></div>
        
        <div id="quoteResults" style="display: none;">
            <h3>📊 見積もり結果</h3>
            <div id="quoteList"></div>
        </div>
    </div>

    <script>
        let currentRequestId = null;
        let quotes = new Map();
        let pollInterval = null;
        
        // 見積もり依頼を送信
        async function requestQuote() {
            const productName = document.getElementById('productName').value.trim();
            
            if (!productName) {
                alert('商品名を入力してください');
                return;
            }
            
            try {
                // Producer APIにPOSTリクエスト
                const response = await fetch('/quotes/request', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ product: productName })
                });
                
                const result = await response.json();
                currentRequestId = result.requestId;
                
                // ステータス表示
                const statusDiv = document.getElementById('status');
                statusDiv.className = 'status pending';
                statusDiv.textContent = `見積もり依頼を送信しました (ID: ${currentRequestId})`;
                statusDiv.style.display = 'block';
                
                // 結果表示エリアを表示
                document.getElementById('quoteResults').style.display = 'block';
                
                // ポーリングを開始
                startPolling();
                
            } catch (error) {
                console.error('見積もり依頼の送信に失敗:', error);
            }
        }
        
        // ポーリングを開始（1秒ごとに結果をチェック）
        function startPolling() {
            if (pollInterval) clearInterval(pollInterval);
            
            pollInterval = setInterval(async () => {
                try {
                    const response = await fetch('/quotes/results');
                    if (response.ok) {
                        const results = await response.json();
                        
                        // 新しい見積もり結果をチェック
                        for (const [requestId, quote] of Object.entries(results)) {
                            if (!quotes.has(requestId)) {
                                quotes.set(requestId, quote);
                                
                                // 現在の依頼に対する見積もり結果の場合、UIを更新
                                if (currentRequestId && quote.requestId === currentRequestId) {
                                    updateQuoteDisplay(quote);
                                    stopPolling(); // 見積もり完了したらポーリングを停止
                                }
                            }
                        }
                    }
                } catch (error) {
                    console.error('見積もり結果の取得に失敗:', error);
                }
            }, 1000);
        }
        
        // 見積もり結果の表示を更新
        function updateQuoteDisplay(quote) {
            const statusDiv = document.getElementById('status');
            const quoteListDiv = document.getElementById('quoteList');
            
            // ステータスを完了に更新
            statusDiv.className = 'status completed';
            statusDiv.textContent = `見積もり完了: ${quote.product} - ¥${quote.price}`;
            
            // 見積もり結果をリストに追加
            const quoteItem = document.createElement('div');
            quoteItem.className = 'quote-item';
            quoteItem.innerHTML = `
                <div>
                    <div class="quote-product">${quote.product}</div>
                    <div class="quote-time">${new Date(quote.timestamp).toLocaleString()}</div>
                </div>
                <div class="quote-price">¥${quote.price}</div>
            `;
            
            quoteListDiv.appendChild(quoteItem);
        }
        
        // ページ読み込み時に既存の見積もり結果を読み込み
        window.addEventListener('load', async function() {
            try {
                const response = await fetch('/quotes/results');
                if (response.ok) {
                    const results = await response.json();
                    
                    // 既存の見積もり結果を表示
                    if (Object.keys(results).length > 0) {
                        document.getElementById('quoteResults').style.display = 'block';
                        
                        for (const [requestId, quote] of Object.entries(results)) {
                            quotes.set(requestId, quote);
                            
                            const quoteListDiv = document.getElementById('quoteList');
                            const quoteItem = document.createElement('div');
                            quoteItem.className = 'quote-item';
                            quoteItem.innerHTML = `
                                <div>
                                    <div class="quote-product">${quote.product}</div>
                                    <div class="quote-time">${new Date(quote.timestamp).toLocaleString()}</div>
                                </div>
                                <div class="quote-price">¥${quote.price}</div>
                            `;
                            
                            quoteListDiv.appendChild(quoteItem);
                        }
                    }
                }
            } catch (error) {
                console.error('既存の見積もり結果の取得に失敗:', error);
            }
        });
    </script>
</body>
</html>
```

**役割**:
- ユーザーインターフェースの提供
- 見積もり依頼の送信
- ポーリングによる見積もり結果の取得・表示
- 既存結果の表示

### 9. **build.gradle.kts** (Gradle設定)
```kotlin
plugins {
    java
    id("io.quarkus")
}

repositories {
    mavenCentral()
    mavenLocal()
}

val quarkusPlatformGroupId: String by project
val quarkusPlatformArtifactId: String by project
val quarkusPlatformVersion: String by project

dependencies {
    implementation(enforcedPlatform("${quarkusPlatformGroupId}:${quarkusPlatformArtifactId}:${quarkusPlatformVersion}"))
    implementation("io.quarkus:quarkus-rest-jackson")      // REST API + JSON
    implementation("io.quarkus:quarkus-messaging-rabbitmq") // RabbitMQ
    implementation("io.quarkus:quarkus-arc")              // CDI
    testImplementation("io.quarkus:quarkus-junit5")
}

group = "org.acme"
version = "1.0.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}
```

**役割**:
- Quarkusプラグインの設定
- 必要な依存関係の定義
- Java 17の設定

---

## 🔧 技術スタック

### **フレームワーク・ライブラリ**
- **Quarkus**: Javaマイクロサービスフレームワーク
- **MicroProfile Reactive Messaging**: 非同期メッセージングAPI
- **SmallRye RabbitMQ**: RabbitMQコネクタ
- **RESTEasy**: REST API実装
- **Jackson**: JSONシリアライゼーション
- **Vert.x**: 非同期I/Oフレームワーク

### **インフラストラクチャ**
- **RabbitMQ**: メッセージブローカー
- **Docker**: コンテナ化
- **Gradle**: ビルドツール
- **OpenJDK 17**: Java実行環境

### **フロントエンド**
- **HTML5**: マークアップ
- **CSS3**: スタイリング
- **JavaScript (ES6+)**: クライアントサイドロジック
- **Fetch API**: HTTP通信

---

## 🎯 学習ポイント

### **非同期メッセージング**
- マイクロサービス間の疎結合通信
- メッセージブローカーによる信頼性の高い通信
- イベント駆動アーキテクチャの実装

### **Quarkus開発**
- 開発モードでのライブリロード
- Dev Servicesによる自動インフラ設定
- ネイティブコンパイル対応

### **問題解決**
- 設定エラーの診断と修正
- 型安全性の問題解決
- 段階的な機能実装

---

## 🚀 今後の拡張可能性

1. **データベース連携**: 見積もり結果の永続化
2. **認証・認可**: ユーザー管理機能
3. **リアルタイム通知**: WebSocketやSSEの実装
4. **マイクロサービス化**: より細かいサービス分割
5. **監視・ログ**: メトリクス収集とログ管理
6. **CI/CD**: 自動デプロイパイプライン

---

## 📚 参考資料

- [Quarkus RabbitMQ Guide](https://ja.quarkus.io/guides/rabbitmq)
- [MicroProfile Reactive Messaging](https://microprofile.io/project/eclipse/microprofile-reactive-messaging)
- [RabbitMQ Documentation](https://www.rabbitmq.com/documentation.html)
- [Quarkus Documentation](https://quarkus.io/guides/)

---

*このドキュメントは、Quarkus RabbitMQ見積もりシステムの完全な実装と解説を提供します。*
