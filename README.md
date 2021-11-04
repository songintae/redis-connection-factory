# redis-connection-factory
### 레디스 커넥션 펙토리(Lettuce) 설정 변경
Aws Master(Writer&read)/Replica(Read)를 활용할 수 있도록 설정하자.

### 참고 자료
1. Spring Data Redis 문서 : https://docs.spring.io/spring-data/redis/docs/2.5.2/reference/html/#redis:write-to-master-read-from-replica
2. Spring에서 기본 RedisConnection 생성 설정 파일 : LettuceConnectionConfiguration.java
3. Lettuce에서 커넥션 pool은 특별한 상황에서 필요함 (lettuce 참고문서) : https://lettuce.io/core/release/reference/index.html#connection-pooling.is-connection-pooling-necessary
