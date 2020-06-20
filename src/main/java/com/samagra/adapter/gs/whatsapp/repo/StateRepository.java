package com.samagra.adapter.gs.whatsapp.repo;

import com.samagra.adapter.gs.whatsapp.entity.GupshupStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StateRepository extends JpaRepository<GupshupStateEntity, Long> {
  GupshupStateEntity findByPhoneNo(String phoneNo);
}

