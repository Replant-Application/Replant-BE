import sys
import subprocess

# Install pymysql if not present
try:
    import pymysql
except ImportError:
    subprocess.check_call([sys.executable, '-m', 'pip', 'install', 'pymysql'])
    import pymysql

def reset_and_sync_schema():
    print("Connecting to database...")
    try:
        conn = pymysql.connect(
            host='113.198.66.75',
            port=13150,
            user='replant_admin',
            password='replant2025',
            database='replant',
            charset='utf8mb4',
            cursorclass=pymysql.cursors.DictCursor
        )
    except Exception as e:
        print(f"Connection failed: {e}")
        return

    try:
        with conn.cursor() as cursor:
            print("Disabling FK checks...")
            cursor.execute("SET FOREIGN_KEY_CHECKS=0;")
            
            # Drop tables to force re-creation (or clean up)
            print("Dropping todolist related tables...")
            cursor.execute("DROP TABLE IF EXISTS todolist_mission")
            cursor.execute("DROP TABLE IF EXISTS todolist_review")
            cursor.execute("DROP TABLE IF EXISTS todolist")
            
            # Note: We are wiping data. But user asked for it ("테이블을 삭제하고...").
            # JPA ddl-auto=update in application.yml might create tables on restart, 
            # OR we can create them manually here to be sure.
            # Given ddl-auto=update is set, restarting the server should create them.
            # But to be safe and instant, let's create them manually based on what we know, 
            # OR just let the server restart handle it. 
            # Since the user is running ./gradlew bootRun, restarting it is needed anyway to pick up schema changes if ddl-auto does it.
            # BUT ddl-auto=update only adds, doesn't fix mismatches easily if table exists. 
            # Dropping them ensures ddl-auto will create fresh correct tables.
            
            print("Tables dropped. Please restart the backend server to let Hibernate create fresh tables.")
            
            print("Enabling FK checks...")
            cursor.execute("SET FOREIGN_KEY_CHECKS=1;")
        
        conn.commit()
        print("Reset completed.")

    except Exception as e:
        print(f"Reset error: {e}")
    finally:
        conn.close()

if __name__ == "__main__":
    reset_and_sync_schema()
