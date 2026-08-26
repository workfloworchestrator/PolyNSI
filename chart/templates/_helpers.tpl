{{/*
Expand the name of the chart.
*/}}
{{- define "PolyNSI.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "PolyNSI.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "PolyNSI.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "PolyNSI.labels" -}}
helm.sh/chart: {{ include "PolyNSI.chart" . }}
{{ include "PolyNSI.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "PolyNSI.selectorLabels" -}}
app.kubernetes.io/name: {{ include "PolyNSI.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "PolyNSI.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "PolyNSI.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/* Resolve the server identity Secret without selecting certificate.secretName when cert-manager is disabled. */}}
{{- define "PolyNSI.serverIdentitySecretName" -}}
{{- if .Values.config.tls.server.identitySecretName -}}
{{- .Values.config.tls.server.identitySecretName -}}
{{- else if and .Values.certificate.enabled .Values.config.tls.server.identity.useCertificateSecret -}}
{{- .Values.certificate.secretName -}}
{{- end -}}
{{- end }}

{{/* Resolve the client identity Secret without selecting certificate.secretName when cert-manager is disabled. */}}
{{- define "PolyNSI.clientIdentitySecretName" -}}
{{- if .Values.config.tls.client.identitySecretName -}}
{{- .Values.config.tls.client.identitySecretName -}}
{{- else if and .Values.certificate.enabled .Values.config.tls.client.identity.useCertificateSecret -}}
{{- .Values.certificate.secretName -}}
{{- end -}}
{{- end }}

{{/* Fail closed for enabled TLS modes and reject the removed JKS configuration. */}}
{{- define "PolyNSI.validateTls" -}}
{{- if hasKey .Values.config "keystore" -}}
{{- fail "config.keystore was removed; provide config.tls.server.identitySecretName and PEM files instead" -}}
{{- end -}}
{{- if hasKey .Values.config "truststore" -}}
{{- fail "config.truststore was removed; provide config.tls.*.caSecretName and PEM files instead" -}}
{{- end -}}
{{- if .Values.config.tls.server.enabled -}}
{{- $_ := required "config.tls.server identity Secret is required when server TLS is enabled" (include "PolyNSI.serverIdentitySecretName" .) -}}
{{- $_ := required "config.tls.server.caSecretName is required when server TLS is enabled" .Values.config.tls.server.caSecretName -}}
{{- end -}}
{{- if .Values.config.tls.client.enabled -}}
{{- $_ := required "config.tls.client identity Secret is required when client TLS is enabled" (include "PolyNSI.clientIdentitySecretName" .) -}}
{{- $_ := required "config.tls.client.caSecretName is required when client TLS is enabled" .Values.config.tls.client.caSecretName -}}
{{- end -}}
{{- end }}
