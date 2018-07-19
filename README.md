# What is this?
Kubernetes Job operator for Digdag.

## how to use.
look `sample_job.yaml`

### on GKE Cluster.
`kubernetes-client/java` doesn't support token refresh now.
but this operator can refresh via `kubectl`.
if you need token refresh, setup `gcloud` and `kubectl`.

## 開発

### ローカルで実行

プラグインのローカルへの登録.
```
./sbt publishM2
```

ローカルへプラグインを登録し直した場合は以下のキャッシュを削除してください
```
rm -rf .digdag/plugins/jp/co/septeni_original/k8sop
```

`sample_job.yaml`のローカル実行.
```
digdag run --no-save sample.dig
```


## 使用上の注意
- namespaceが指定されない場合、jobは `default`namespaceで実行されます。
