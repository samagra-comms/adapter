package com.samagra.adapter.gs.whatsapp.repo;

import com.samagra.adapter.gs.whatsapp.entity.GupshupMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends JpaRepository<GupshupMessageEntity, Long> {
  GupshupMessageEntity findByPhoneNo(String phoneNo);
}
