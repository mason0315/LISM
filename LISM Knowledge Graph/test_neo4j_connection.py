from neo4j import GraphDatabase
import socket

# Neo4j数据库连接配置
NEO4J_URI = "neo4j://127.0.0.1:7687"
NEO4J_USER = "neo4j"
NEO4J_PASSWORD = "12345678"

def check_port_connection(host="127.0.0.1", port=7687):
    """检查端口连接"""
    try:
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.settimeout(5)
        result = sock.connect_ex((host, port))
        sock.close()
        if result == 0:
            print(f"✓ 端口 {host}:{port} 可访问")
            return True
        else:
            print(f"✗ 端口 {host}:{port} 不可访问，错误代码: {result}")
            return False
    except Exception as e:
        print(f"检查端口连接时出错: {e}")
        return False

def test_neo4j_connection():
    """测试Neo4j数据库连接"""
    print("检查Neo4j数据库连接...")
    
    # 首先检查端口
    if not check_port_connection():
        print("请确保Neo4j数据库已启动并监听在端口7687上")
        return
    
    # 尝试连接数据库
    try:
        print("尝试连接到Neo4j数据库...")
        driver = GraphDatabase.driver(NEO4J_URI, auth=(NEO4J_USER, NEO4J_PASSWORD))
        with driver.session() as session:
            result = session.run("RETURN 'Hello, Neo4j!' as message")
            record = result.single()
            print(f"✓ 数据库连接成功: {record['message']}")
        driver.close()
    except Exception as e:
        print(f"✗ 数据库连接失败: {e}")

if __name__ == "__main__":
    test_neo4j_connection()