package com.schoolsaas.repository;

import com.schoolsaas.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {
    List<Conversation> findBySchoolIdOrderByUpdatedAtDesc(UUID schoolId);

    @Query("SELECT c FROM Conversation c JOIN ConversationParticipant cp ON c.id = cp.conversationId WHERE cp.userId = :userId AND c.schoolId = :schoolId ORDER BY c.updatedAt DESC")
    List<Conversation> findByParticipantUserIdAndSchoolId(UUID userId, UUID schoolId);

    @Query("SELECT c FROM Conversation c WHERE c.type = 'DIRECT' AND c.schoolId = :schoolId AND EXISTS (SELECT 1 FROM ConversationParticipant cp1 WHERE cp1.conversationId = c.id AND cp1.userId = :userId1) AND EXISTS (SELECT 1 FROM ConversationParticipant cp2 WHERE cp2.conversationId = c.id AND cp2.userId = :userId2)")
    Optional<Conversation> findDirectConversation(UUID schoolId, UUID userId1, UUID userId2);
}
