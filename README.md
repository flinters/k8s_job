# What is this?
Kubernetes Job operator for Digdag.

## how to use.
look `sample_job.yaml`

### args
- timeout:
  - 数値
  - 必須
  - ジョブの実行時間にタイムアウトを設定します。
- cmdir
  - 文字列配列
  - 任意
  - ConfigMapを指定されたディレクトリのファイルから作成します。

### ConfigMap from Directory
`cmdir`で指定したディレクトリに`base.yaml`を配置してください。
`base.yaml`で指定された情報を元に、ディレクトリ内のファイルの内容をファイル名をキーとしてConfigMapが作成されます。

#### 制限
- ConfigMapが作成されるのは、指定されたディレクトリの直下のファイルのみです。

### Supported Resource.
- core/ConfigMap
- batch/Job

### on GKE Cluster.
`kubernetes-client/java` doesn't support token refresh now.
but this operator can refresh via `kubectl`.
if you need token refresh, setup `gcloud` and `kubectl`.

## 開発

### ローカルで実行

#### プラグインのローカルへの登録.
```
./sbt publishM2
```

#### ローカルへプラグインを登録し直した場合は以下のキャッシュを削除してください
```
rm -rf .digdag/plugins/jp/co/septeni_original/k8sop
```

#### ローカルリポジトリを参照
`sample_job.yaml`をいかに差し替え
```yaml
_export:
  plugin:
    repositories:
    - file://[USER_HOME]/.m2/repository/
    dependencies:
    - jp.co.septeni_original:k8sop_2.12:0.1-SNAPSHOT
```

`sample_job.yaml`のローカル実行.
リポジトリ、
```
digdag run --no-save sample.dig
```


## 使用上の注意
- namespaceが指定されない場合、jobは `default`namespaceで実行されます。
