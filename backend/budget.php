<?php
require_once 'db_config.php';

// Ensure tables exist
$conn->query("CREATE TABLE IF NOT EXISTS trip_budgets (
    id INT AUTO_INCREMENT PRIMARY KEY,
    trip_title VARCHAR(255) NOT NULL,
    total_budget DECIMAL(10, 2) DEFAULT 0,
    UNIQUE(trip_title)
)");

$conn->query("CREATE TABLE IF NOT EXISTS expenses (
    id INT AUTO_INCREMENT PRIMARY KEY,
    trip_title VARCHAR(255) NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    note VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
)");

$action = $_GET['action'] ?? '';
$trip = $_GET['trip'] ?? '';

if (empty($trip)) {
    die(json_encode(["status" => "error", "message" => "No trip specified"]));
}

if ($action == 'get') {
    // Get budget
    $stmt = $conn->prepare("SELECT total_budget FROM trip_budgets WHERE trip_title = ?");
    $stmt->bind_param("s", $trip);
    $stmt->execute();
    $res = $stmt->get_result();
    $row = $res->fetch_assoc();
    $budget = $row ? (float)$row['total_budget'] : 0.0;

    // Get expenses
    $stmt = $conn->prepare("SELECT * FROM expenses WHERE trip_title = ? ORDER BY created_at DESC");
    $stmt->bind_param("s", $trip);
    $stmt->execute();
    $res = $stmt->get_result();
    $expenses = [];
    while ($r = $res->fetch_assoc()) {
        $r['amount'] = (float)$r['amount'];
        $expenses[] = $r;
    }

    echo json_encode([
        "status" => "success",
        "total_budget" => $budget,
        "expenses" => $expenses
    ]);
}
elseif ($action == 'set_limit') {
    $limit = $_GET['limit'] ?? 0;
    $stmt = $conn->prepare("INSERT INTO trip_budgets (trip_title, total_budget) VALUES (?, ?) ON DUPLICATE KEY UPDATE total_budget = ?");
    $stmt->bind_param("sdd", $trip, $limit, $limit);
    if ($stmt->execute()) {
        echo json_encode(["status" => "success"]);
    }
}
elseif ($action == 'add_expense') {
    $amount = $_GET['amount'] ?? 0;
    $note = $_GET['note'] ?? '';
    $stmt = $conn->prepare("INSERT INTO expenses (trip_title, amount, note) VALUES (?, ?, ?)");
    $stmt->bind_param("sds", $trip, $amount, $note);
    if ($stmt->execute()) {
        echo json_encode(["status" => "success"]);
    }
}
elseif ($action == 'delete_expense') {
    $id = $_GET['id'] ?? 0;
    $stmt = $conn->prepare("DELETE FROM expenses WHERE id = ?");
    $stmt->bind_param("i", $id);
    if ($stmt->execute()) {
        echo json_encode(["status" => "success"]);
    }
}
?>
