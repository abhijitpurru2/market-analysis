{{- define "market.labels" -}}
app.kubernetes.io/managed-by: {{ .Release.Service }}
helm.sh/chart: {{ .Chart.Name }}-{{ .Chart.Version }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{- define "market.image" -}}
{{- if .Values.image.registry -}}
{{ .Values.image.registry }}/{{ . }}:{{ $.Values.image.tag }}
{{- else -}}
market/{{ . }}:{{ $.Values.image.tag }}
{{- end -}}
{{- end }}
