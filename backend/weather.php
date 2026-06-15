<?php
require_once 'config.php';

header('Content-Type: application/json');

$place = $_GET['place'] ?? '';

if (empty($place)) {
    die(json_encode(["status" => "error", "message" => "No place specified"]));
}

$cache_file = "cache/weather_" . md5(strtolower($place)) . ".json";
$cache_time = 3 * 60 * 60; // 3 hours cache

if (file_exists($cache_file) && (time() - filemtime($cache_file) < $cache_time)) {
    $cached_data = file_get_contents($cache_file);
    if (!empty($cached_data)) {
        echo trim($cached_data);
        exit;
    }
}

$prompt = "Provide current and forecast weather for $place. 
Return ONLY a valid JSON object with this exact structure:
{
  \"current_temp\": \"28°\",
  \"condition\": \"Sunny\",
  \"hourly\": [
    {\"time\": \"10 AM\", \"temp\": \"22°\", \"condition\": \"Sunny\"},
    {\"time\": \"11 AM\", \"temp\": \"23°\", \"condition\": \"Sunny\"},
    {\"time\": \"12 PM\", \"temp\": \"24°\", \"condition\": \"Sunny\"},
    {\"time\": \"1 PM\", \"temp\": \"25°\", \"condition\": \"Sunny\"},
    {\"time\": \"2 PM\", \"temp\": \"26°\", \"condition\": \"Sunny\"}
  ],
  \"daily\": [
    {\"day\": \"Wed\", \"low\": \"18°\", \"high\": \"22°\", \"condition\": \"Rainy\"},
    {\"day\": \"Thu\", \"low\": \"19°\", \"high\": \"24°\", \"condition\": \"Cloudy\"},
    {\"day\": \"Fri\", \"low\": \"20°\", \"high\": \"26°\", \"condition\": \"Sunny\"},
    {\"day\": \"Sat\", \"low\": \"21°\", \"high\": \"27°\", \"condition\": \"Sunny\"},
    {\"day\": \"Sun\", \"low\": \"22°\", \"high\": \"28°\", \"condition\": \"Sunny\"},
    {\"day\": \"Mon\", \"low\": \"21°\", \"high\": \"26°\", \"condition\": \"Cloudy\"},
    {\"day\": \"Tue\", \"low\": \"20°\", \"high\": \"24°\", \"condition\": \"Rainy\"}
  ]
}
Use real-time accurate data for $place.";

$response = callGemini($prompt, true);
$weather_data = null;

if ($response['success']) {
    $clean_json = trim($response['text']);
    $weather_data = json_decode($clean_json, true);
}

if ($weather_data !== null && isset($weather_data['current_temp'])) {
    if (!is_dir('cache')) {
        mkdir('cache', 0777, true);
    }
    $final_output = json_encode($weather_data);
    file_put_contents($cache_file, $final_output);
    echo $final_output;
    exit;
}

// Fallback if API fails or returns invalid response
$fallback_weather = [
    "current_temp" => "26°",
    "condition" => "Partly Cloudy",
    "hourly" => [
        ["time" => "10 AM", "temp" => "24°", "condition" => "Partly Cloudy"],
        ["time" => "11 AM", "temp" => "25°", "condition" => "Partly Cloudy"],
        ["time" => "12 PM", "temp" => "26°", "condition" => "Partly Cloudy"],
        ["time" => "1 PM", "temp" => "27°", "condition" => "Partly Cloudy"],
        ["time" => "2 PM", "temp" => "27°", "condition" => "Partly Cloudy"]
    ],
    "daily" => [
        ["day" => "Wed", "low" => "20°", "high" => "26°", "condition" => "Partly Cloudy"],
        ["day" => "Thu", "low" => "19°", "high" => "25°", "condition" => "Partly Cloudy"],
        ["day" => "Fri", "low" => "21°", "high" => "27°", "condition" => "Sunny"],
        ["day" => "Sat", "low" => "22°", "high" => "28°", "condition" => "Sunny"],
        ["day" => "Sun", "low" => "22°", "high" => "28°", "condition" => "Sunny"],
        ["day" => "Mon", "low" => "21°", "high" => "26°", "condition" => "Partly Cloudy"],
        ["day" => "Tue", "low" => "20°", "high" => "25°", "condition" => "Partly Cloudy"]
    ]
];

if (!is_dir('cache')) {
    mkdir('cache', 0777, true);
}
$final_output = json_encode($fallback_weather);
file_put_contents($cache_file, $final_output);
echo $final_output;
exit;
?>
