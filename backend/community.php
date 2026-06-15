<?php
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");
header("Access-Control-Allow-Methods: GET, POST, OPTIONS");
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    exit(0);
}
include 'db_config.php';

// Ensure community_posts table exists
$table_sql = "CREATE TABLE IF NOT EXISTS community_posts (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_name VARCHAR(100) NOT NULL,
    user_rating VARCHAR(100) NOT NULL,
    avatar_id INT NOT NULL,
    destination VARCHAR(255) NOT NULL,
    dates VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    interested_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
)";
$conn->query($table_sql);

// Ensure join_requests table exists
$jr_table_sql = "CREATE TABLE IF NOT EXISTS join_requests (
    id INT AUTO_INCREMENT PRIMARY KEY,
    post_id INT NOT NULL,
    requester_name VARCHAR(100) NOT NULL,
    requester_email VARCHAR(100) NOT NULL,
    destination VARCHAR(255) NOT NULL,
    status VARCHAR(20) DEFAULT 'pending',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
)";
$conn->query($jr_table_sql);

// Dynamically alter user_rating to VARCHAR(100) in case the table was already created with VARCHAR(10)
$conn->query("ALTER TABLE community_posts MODIFY user_rating VARCHAR(100) NOT NULL");

// Ensure any pre-existing default mock data is removed, and do not seed default mock data
$conn->query("DELETE FROM community_posts WHERE user_name IN ('Sarah Jenkins', 'Mike Chen')");

$action = $_GET['action'] ?? 'get';

if ($action === 'get') {
    $result = $conn->query("SELECT * FROM community_posts ORDER BY created_at DESC");
    $posts = [];
    while ($row = $result->fetch_assoc()) {
        $posts[] = [
            "id" => (int)$row['id'],
            "user_name" => $row['user_name'],
            "user_rating" => $row['user_rating'],
            "avatar_id" => (int)$row['avatar_id'],
            "destination" => $row['destination'],
            "dates" => $row['dates'],
            "description" => $row['description'],
            "interested_count" => (int)$row['interested_count']
        ];
    }
    echo json_encode(["status" => "success", "data" => $posts]);
    exit;
}

if ($action === 'add') {
    $user_name = $_POST['user_name'] ?? 'Traveler';
    $destination = $_POST['destination'] ?? '';
    $dates = $_POST['dates'] ?? '';
    $description = $_POST['description'] ?? '';
    $avatar_id = rand(1, 5); // Cycle through avatars
    $user_rating = "4." . rand(5, 9) . " Traveler Rating";

    if (empty($destination) || empty($description)) {
        echo json_encode(["status" => "error", "message" => "Destination and description are required"]);
        exit;
    }

    $stmt = $conn->prepare("INSERT INTO community_posts (user_name, user_rating, avatar_id, destination, dates, description) VALUES (?, ?, ?, ?, ?, ?)");
    $stmt->bind_param("ssisss", $user_name, $user_rating, $avatar_id, $destination, $dates, $description);
    
    if ($stmt->execute()) {
        echo json_encode(["status" => "success", "message" => "Post added successfully"]);
    } else {
        echo json_encode(["status" => "error", "message" => "Failed to add post"]);
    }
    exit;
}

if ($action === 'join') {
    $id = (int)($_POST['id'] ?? 0);
    $requester_name = $_POST['requester_name'] ?? '';
    $requester_email = $_POST['requester_email'] ?? '';

    if ($id <= 0 || empty($requester_name) || empty($requester_email)) {
        echo json_encode(["status" => "error", "message" => "Missing required fields"]);
        exit;
    }

    $post_res = $conn->query("SELECT destination FROM community_posts WHERE id = $id");
    if ($post_res->num_rows === 0) {
        echo json_encode(["status" => "error", "message" => "Post not found"]);
        exit;
    }
    $post_data = $post_res->fetch_assoc();
    $destination = $post_data['destination'];

    // Insert into join_requests table
    $stmt = $conn->prepare("INSERT INTO join_requests (post_id, requester_name, requester_email, destination) VALUES (?, ?, ?, ?)");
    $stmt->bind_param("isss", $id, $requester_name, $requester_email, $destination);
    $stmt->execute();

    // Update community_posts interested count
    $conn->query("UPDATE community_posts SET interested_count = interested_count + 1 WHERE id = $id");

    echo json_encode(["status" => "success", "message" => "Joined successfully"]);
    exit;
}

if ($action === 'get_requests') {
    $owner_name = $_GET['owner_name'] ?? '';
    if (empty($owner_name)) {
        echo json_encode(["status" => "error", "message" => "Owner name is required"]);
        exit;
    }

    $stmt = $conn->prepare("
        SELECT jr.* 
        FROM join_requests jr
        JOIN community_posts cp ON jr.post_id = cp.id
        WHERE cp.user_name = ? AND jr.status = 'pending'
        ORDER BY jr.created_at DESC
    ");
    $stmt->bind_param("s", $owner_name);
    $stmt->execute();
    $result = $stmt->get_result();

    $requests = [];
    while ($row = $result->fetch_assoc()) {
        $requests[] = [
            "id" => (int)$row['id'],
            "post_id" => (int)$row['post_id'],
            "requester_name" => $row['requester_name'],
            "requester_email" => $row['requester_email'],
            "destination" => $row['destination'],
            "status" => $row['status']
        ];
    }
    echo json_encode(["status" => "success", "data" => $requests]);
    exit;
}

if ($action === 'respond_request') {
    $request_id = (int)($_POST['request_id'] ?? 0);
    $status = $_POST['status'] ?? ''; // 'accepted' or 'declined'

    if ($request_id <= 0 || !in_array($status, ['accepted', 'declined'])) {
        echo json_encode(["status" => "error", "message" => "Invalid parameters"]);
        exit;
    }

    $stmt = $conn->prepare("UPDATE join_requests SET status = ? WHERE id = ?");
    $stmt->bind_param("si", $status, $request_id);
    if ($stmt->execute()) {
        echo json_encode(["status" => "success", "message" => "Request updated successfully"]);
    } else {
        echo json_encode(["status" => "error", "message" => "Failed to update request"]);
    }
    exit;
}

if ($action === 'edit') {
    $id = (int)($_POST['id'] ?? 0);
    $destination = $_POST['destination'] ?? '';
    $dates = $_POST['dates'] ?? '';
    $description = $_POST['description'] ?? '';

    if ($id <= 0 || empty($destination) || empty($description)) {
        echo json_encode(["status" => "error", "message" => "All fields are required"]);
        exit;
    }

    $stmt = $conn->prepare("UPDATE community_posts SET destination = ?, dates = ?, description = ? WHERE id = ?");
    $stmt->bind_param("sssi", $destination, $dates, $description, $id);
    
    if ($stmt->execute()) {
        echo json_encode(["status" => "success", "message" => "Post updated successfully"]);
    } else {
        echo json_encode(["status" => "error", "message" => "Failed to update post"]);
    }
    exit;
}

if ($action === 'delete') {
    $id = (int)($_POST['id'] ?? 0);
    if ($id <= 0) {
        echo json_encode(["status" => "error", "message" => "Invalid post ID"]);
        exit;
    }

    $stmt = $conn->prepare("DELETE FROM community_posts WHERE id = ?");
    $stmt->bind_param("i", $id);
    
    if ($stmt->execute()) {
        echo json_encode(["status" => "success", "message" => "Post deleted successfully"]);
    } else {
        echo json_encode(["status" => "error", "message" => "Failed to delete post"]);
    }
    exit;
}

echo json_encode(["status" => "error", "message" => "Invalid action"]);
?>
