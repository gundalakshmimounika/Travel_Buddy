<?php
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");
header("Access-Control-Allow-Methods: GET, POST, OPTIONS");
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    exit(0);
}
include 'db_config.php';

// Ensure alerts table exists
$table_sql = "CREATE TABLE IF NOT EXISTS alerts (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_email VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(50) NOT NULL,
    timestamp BIGINT NOT NULL
)";
$conn->query($table_sql);

$action = $_GET['action'] ?? 'get';

if ($action === 'get') {
    $email = $_GET['email'] ?? '';
    if (empty($email)) {
        echo json_encode(["status" => "error", "message" => "Email is required"]);
        exit;
    }

    $stmt = $conn->prepare("SELECT * FROM alerts WHERE user_email = ? ORDER BY timestamp DESC");
    $stmt->bind_param("s", $email);
    $stmt->execute();
    $result = $stmt->get_result();

    $alerts = [];
    while ($row = $result->fetch_assoc()) {
        $alerts[] = [
            "id" => (int)$row['id'],
            "user_email" => $row['user_email'],
            "title" => $row['title'],
            "message" => $row['message'],
            "type" => $row['type'],
            "timestamp" => (float)$row['timestamp']
        ];
    }
    echo json_encode(["status" => "success", "data" => $alerts]);
    exit;
}

if ($action === 'add') {
    // Read POST variables
    $email = $_POST['email'] ?? '';
    $title = $_POST['title'] ?? '';
    $message = $_POST['message'] ?? '';
    $type = $_POST['type'] ?? 'general';
    $timestamp = round(microtime(true) * 1000); // current epoch millis

    if (empty($email) || empty($title) || empty($message)) {
        // Support JSON input too
        $json = file_get_contents('php://input');
        $data = json_decode($json, true);
        if ($data) {
            $email = $data['email'] ?? '';
            $title = $data['title'] ?? '';
            $message = $data['message'] ?? '';
            $type = $data['type'] ?? 'general';
        }
    }

    if (empty($email) || empty($title) || empty($message)) {
        echo json_encode(["status" => "error", "message" => "Missing required fields"]);
        exit;
    }

    $stmt = $conn->prepare("INSERT INTO alerts (user_email, title, message, type, timestamp) VALUES (?, ?, ?, ?, ?)");
    $stmt->bind_param("ssssi", $email, $title, $message, $type, $timestamp);
    
    if ($stmt->execute()) {
        echo json_encode(["status" => "success", "message" => "Alert added successfully"]);
    } else {
        echo json_encode(["status" => "error", "message" => "Failed to add alert"]);
    }
    exit;
}

if ($action === 'clear') {
    $email = $_GET['email'] ?? '';
    if (empty($email)) {
        echo json_encode(["status" => "error", "message" => "Email is required"]);
        exit;
    }

    $stmt = $conn->prepare("DELETE FROM alerts WHERE user_email = ?");
    $stmt->bind_param("s", $email);
    
    if ($stmt->execute()) {
        echo json_encode(["status" => "success", "message" => "Alerts cleared successfully"]);
    } else {
        echo json_encode(["status" => "error", "message" => "Failed to clear alerts"]);
    }
    exit;
}

if ($action === 'delete') {
    $id = (int)($_GET['id'] ?? 0);
    if ($id <= 0) {
        echo json_encode(["status" => "error", "message" => "Invalid alert ID"]);
        exit;
    }

    $stmt = $conn->prepare("DELETE FROM alerts WHERE id = ?");
    $stmt->bind_param("i", $id);
    
    if ($stmt->execute()) {
        echo json_encode(["status" => "success", "message" => "Alert deleted successfully"]);
    } else {
        echo json_encode(["status" => "error", "message" => "Failed to delete alert"]);
    }
    exit;
}

echo json_encode(["status" => "error", "message" => "Invalid action"]);
?>
