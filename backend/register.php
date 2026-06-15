<?php
require_once 'db_config.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    echo json_encode(["status" => "error", "message" => "Invalid request method"]);
    exit;
}

$full_name = isset($_POST['full_name']) ? trim($_POST['full_name']) : '';
$email = isset($_POST['email']) ? trim($_POST['email']) : '';
$password = isset($_POST['password']) ? $_POST['password'] : '';

if (empty($full_name) || empty($email) || empty($password)) {
    echo json_encode(["status" => "error", "message" => "All fields are required"]);
    exit;
}

// 1. Email must end with "@gmail.com"
if (substr(strtolower($email), -10) !== '@gmail.com') {
    echo json_encode(["status" => "error", "message" => "Email must end with @gmail.com"]);
    exit;
}

// 2. Password must be longer than 6 digits/letters (length > 6)
if (strlen($password) <= 6) {
    echo json_encode(["status" => "error", "message" => "Password must be longer than 6 characters"]);
    exit;
}

// 3. Password must contain at least 1 uppercase letter
if (!preg_match('/[A-Z]/', $password)) {
    echo json_encode(["status" => "error", "message" => "Password must contain at least 1 uppercase letter"]);
    exit;
}

// Check if email already registered
$check_stmt = $conn->prepare("SELECT id FROM users WHERE email = ?");
$check_stmt->bind_param("s", $email);
$check_stmt->execute();
$check_stmt->store_result();

if ($check_stmt->num_rows > 0) {
    echo json_encode(["status" => "error", "message" => "This email is already registered"]);
    $check_stmt->close();
    exit;
}
$check_stmt->close();

// Insert user into database
$insert_stmt = $conn->prepare("INSERT INTO users (full_name, email, password) VALUES (?, ?, ?)");
$insert_stmt->bind_param("sss", $full_name, $email, $password);

if ($insert_stmt->execute()) {
    echo json_encode(["status" => "success", "message" => "Account created successfully"]);
} else {
    echo json_encode(["status" => "error", "message" => "Failed to create account"]);
}

$insert_stmt->close();
$conn->close();
?>
