<?php
// Enable CORS for local web client integrations
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");
header("Access-Control-Allow-Methods: GET, POST, OPTIONS");
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    exit(0);
}

// Global Configuration for Travel Buddy Backend
define('GEMINI_API_KEY', 'AIzaSyC4Vdx6PXg75_xGAfuXiEasVq_kMKBKK5g');

function callGemini($prompt, $jsonMode = false) {
    $url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite:generateContent?key=" . GEMINI_API_KEY;
    
    $payload = [
        "contents" => [
            [
                "parts" => [
                    ["text" => $prompt]
                ]
            ]
        ]
    ];

    if ($jsonMode) {
        $payload["generationConfig"] = [
            "responseMimeType" => "application/json"
        ];
    }

    $attempts = 0;
    $maxAttempts = 2;
    $wait = 2; // Start with 2 second wait

    while ($attempts < $maxAttempts) {
        $ch = curl_init($url);
        curl_setopt($ch, CURLOPT_HTTPHEADER, ['Content-Type: application/json']);
        curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($payload));
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_TIMEOUT, 15);
        curl_setopt($ch, CURLOPT_CONNECTTIMEOUT, 5);
        
        $response = curl_exec($ch);
        $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        curl_close($ch);

        if ($httpCode === 200) {
            $result = json_decode($response, true);
            $text = $result['candidates'][0]['content']['parts'][0]['text'] ?? null;
            if ($text) {
                return ["success" => true, "text" => $text];
            }
            return ["error" => "no_content"];
        }

        if ($httpCode === 429) {
            $attempts++;
            if ($attempts < $maxAttempts) {
                sleep($wait);
                $wait *= 2; // Exponential backoff: 2s, 4s, 8s
                continue;
            }
            return ["error" => "rate_limit"];
        }

        return ["error" => "api_error", "code" => $httpCode];
    }

    return ["error" => "rate_limit"];
}
?>
