<?php
require_once 'db_config.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    echo json_encode(["status" => "error", "message" => "Invalid request method"]);
    exit;
}

$email = isset($_POST['email']) ? trim($_POST['email']) : '';
$password = isset($_POST['password']) ? $_POST['password'] : '';

if (empty($email) || empty($password)) {
    echo json_encode(["status" => "error", "message" => "Email and new password are required"]);
    exit;
}

// 1. Password must be longer than 6 characters (length > 6)
if (strlen($password) <= 6) {
    echo json_encode(["status" => "error", "message" => "Password must be longer than 6 characters"]);
    exit;
}

// 2. Password must contain at least 1 uppercase letter
if (!preg_match('/[A-Z]/', $password)) {
    echo json_encode(["status" => "error", "message" => "Password must contain at least 1 uppercase letter"]);
    exit;
}

// Verify email exists
$check_stmt = $conn->prepare("SELECT id FROM users WHERE email = ?");
$check_stmt->bind_param("s", $email);
$check_stmt->execute();
$check_stmt->store_result();

if ($check_stmt->num_rows === 0) {
    echo json_encode(["status" => "error", "message" => "Email address not found"]);
    $check_stmt->close();
    exit;
}
$check_stmt->close();

// Update password in database
$update_stmt = $conn->prepare("UPDATE users SET password = ? WHERE email = ?");
$update_stmt->bind_param("ss", $password, $email);

if ($update_stmt->execute()) {
    echo json_encode(["status" => "success", "message" => "Password reset successful"]);
} else {
    echo json_encode(["status" => "error", "message" => "Failed to update password"]);
}

$update_stmt->close();
$conn->close();
?>
