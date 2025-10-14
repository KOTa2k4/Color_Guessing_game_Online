# Color_Guessing_game_Online
Hệ thống của hoạt động theo mô hình Client-Server điển hình, được điều phối bởi một kiến trúc hướng dịch vụ (Service-Oriented) và sự kiện (Event-Driven) ở phía server, cùng với kiến trúc MVC (Model-View-Controller) ở phía client.

Luồng hoạt động phía Server 🖥️ (Bộ não)
Đây là luồng xử lý chính khi một người chơi kết nối và tương tác với game.

Kết nối & Xác thực:

GameServer liên tục lắng nghe kết nối mới.

Khi một client kết nối, GameServer tạo một luồng riêng để xử lý (ExecutorService).

Client gửi tin nhắn LOGIN.

GameServer gọi AuthenticationService để kiểm tra username/password với CSDL (thông qua UserDAO). Nếu người dùng không tồn tại, một tài khoản mới sẽ được tạo.

Nếu xác thực thành công, GameServer tạo một đối tượng ClientHandler mới để quản lý kết nối này.

Vào Sảnh chờ (Lobby):

ClientHandler được thêm vào Lobby, nơi lưu danh sách tất cả người chơi đang online.

GameServer yêu cầu BroadcastService gửi danh sách người dùng đã được cập nhật cho tất cả mọi người.

Xử lý Yêu cầu (Luồng tin nhắn chung):

ClientHandler liên tục lắng nghe tin nhắn từ client của nó.

Khi nhận được một tin nhắn (ví dụ: CHALLENGE, CHAT_MESSAGE, MOVE), nó không tự xử lý mà chuyển ngay cho MessageHandler (Tổng đài trung tâm).

MessageHandler xem type của tin nhắn và chuyển nó đến đúng Service chuyên trách:

CHALLENGE ➡️ LobbyService.

MOVE ➡️ MatchService.

CHAT_MESSAGE ➡️ ChatService.

Bắt đầu Trận đấu:

LobbyService xử lý logic thách đấu. Nếu cả hai người chơi đồng ý, nó sẽ gọi MatchService.

MatchService tạo một đối tượng MatchSession mới (logic game thuần túy), "tiêm" chính nó vào làm MatchListener.

MatchSession bắt đầu và quản lý toàn bộ logic của trận đấu (tạo màu, tính điểm...).

Trong Trận đấu (Mô hình Listener):

Khi có một sự kiện trong game (ví dụ: vòng đấu kết thúc), MatchSession không tự mình lưu CSDL hay gửi tin nhắn.

Thay vào đó, nó "thông báo" cho MatchListener (chính là MatchService):

onMatchDataSave(): MatchService nhận sự kiện này và gọi UserDAO để lưu kết quả. Nó cũng cập nhật điểm và số trận thắng trên đối tượng User trong bộ nhớ.

onSendMessage(): MatchService nhận sự kiện và gọi phương thức .send() của ClientHandler tương ứng.

onPlayerStatusUpdate(): MatchService gọi BroadcastService để thông báo cho mọi người trong sảnh chờ về sự thay đổi trạng thái (điểm, số trận thắng).

Luồng hoạt động phía Client 👤 (Giao diện)
Client hoạt động theo mô hình Model-View-Controller (MVC) rất rõ ràng.

Khởi động & Đăng nhập:

GameClientMain tạo và hiển thị LobbyView (cửa sổ chính).

Người dùng nhập thông tin và nhấn "Login".

LobbyView tạo ra GameClient (người đưa thư) và ClientController (bộ não). Nó đăng ký ClientController để lắng nghe mọi tin nhắn từ GameClient.

Một tin nhắn LOGIN được gửi đến server.

Xử lý Tin nhắn đến:

GameClient nhận một tin nhắn từ server (ví dụ: USER_LIST).

Nó không tự xử lý mà ngay lập tức chuyển tin nhắn đó cho ClientController.

ClientController là trung tâm xử lý duy nhất. Nó dùng switch-case để phân tích loại tin nhắn và ra lệnh cho các View tương ứng:

Nhận USER_LIST ➡️ Gọi lobbyView.updateUserList().

Nhận CHAT_MESSAGE ➡️ Gọi lobbyView.appendChatMessage().

Nhận IN_GAME_CHAT ➡️ Gọi gameView.appendChatMessage().

Bắt đầu Trận đấu:

Khi ClientController nhận tin nhắn START_GAME:

Nó tạo một cửa sổ GameView mới (một JDialog).

Nó truyền tất cả dữ liệu cần thiết (tên đối thủ, danh sách màu, điểm số) vào constructor của GameView.

Nó gọi gameView.setVisible(true) để hiển thị cửa sổ game.

Trong Trận đấu:

GameView tự quản lý toàn bộ giao diện và logic hiển thị của nó (bộ đếm giờ, hiệu ứng...).

Khi người chơi thực hiện một hành động (nhấp vào màu, gửi tin chat), GameView sẽ tạo một Message tương ứng và gửi nó đi thông qua GameClient.

Khi ClientController nhận được tin nhắn ROUND_RESULT, nó sẽ gọi gameView.updateScores() để cập nhật điểm số trên giao diện.