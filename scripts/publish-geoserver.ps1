# Publishes workspace 'spatial', PostGIS datastore, and WMS layer 'spatial:spatial_features' to GeoServer REST API
param(
  [string]$GeoServerUrl = "http://localhost:8600/geoserver",
  [string]$User = "admin",
  [string]$Pass = "geoserver"
)

$pair = "${User}:${Pass}"
$auth = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes($pair))
$headers = @{
  "Authorization" = "Basic $auth"
  "Content-Type"  = "application/json"
}

Write-Host "1. Creating workspace 'spatial'..."
Invoke-RestMethod -Uri "$GeoServerUrl/rest/workspaces" -Method Post -Headers $headers -Body '{"workspace":{"name":"spatial"}}' -ErrorAction SilentlyContinue

Write-Host "2. Creating PostGIS datastore 'postgis_store'..."
$dsBody = @{
  dataStore = @{
    name = "postgis_store"
    connectionParameters = @{
      entry = @(
        @{ "@key" = "dbtype"; "$" = "postgis" },
        @{ "@key" = "host"; "$" = "postgis" },
        @{ "@key" = "port"; "$" = "5432" },
        @{ "@key" = "database"; "$" = "spatialdb" },
        @{ "@key" = "schema"; "$" = "public" },
        @{ "@key" = "user"; "$" = "spatial" },
        @{ "@key" = "passwd"; "$" = "spatial1234" }
      )
    }
  }
} | ConvertTo-Json -Depth 6
Invoke-RestMethod -Uri "$GeoServerUrl/rest/workspaces/spatial/datastores" -Method Post -Headers $headers -Body $dsBody -ErrorAction SilentlyContinue

Write-Host "3. Publishing WMS featureType 'spatial_features'..."
$ftBody = @{
  featureType = @{
    name = "spatial_features"
    nativeName = "spatial_features"
    title = "Spatial Facilities & Sensors"
    srs = "EPSG:4326"
    enabled = $true
  }
} | ConvertTo-Json -Depth 4
Invoke-RestMethod -Uri "$GeoServerUrl/rest/workspaces/spatial/datastores/postgis_store/featuretypes" -Method Post -Headers $headers -Body $ftBody -ErrorAction SilentlyContinue

Write-Host "Published WMS layer: $GeoServerUrl/spatial/wms (LAYERS=spatial:spatial_features)"
